#!/usr/bin/env python3
"""Selects the base learner configuration of each ensemble from the tuning runs (results/raw/ensemble_tuning/*.csv,
written by scripts/run_ensemble_tuning.sh) on the streams of configs/ensemble_tuning_datasets.txt (the ablation streams
without hyperA).

The ensembles keep the structure of SGBR's defaults (OzaBag with 10 members; SGBR with 10 boosting steps of OzaBag with
10 members, learning rate 1), and only the base learner (FIMT-DD or BRIM) is tuned. As in select_baselines.py, the
configurations are ranked on each dataset by their adjusted R^2 (mean over seeds 1-3) and the configuration with the best
average rank is selected (ties: higher median adjusted R^2). A run whose adjusted R^2 is NaN diverged (e.g., FIMT-DD
with perceptron leaves inside SGBR); it counts as -infinity, so that its configuration ranks last on that dataset
instead of the dataset being dropped.

The grids are those of the single trees (scripts/gen_ensemble_grids.py); all BRIM configurations re-evaluate their
splits after a drift (-V). Writes results/summary/ensemble_tuning_<grid>.csv, configs/ensemble_methods.tsv (the selected
ensembles, plus the BRIM ensembles with memory budgets of 1 MB and 100 KB per member and without re-evaluation after a
drift) and paper/tables/ensemble_tuning.tex.
"""
import glob
import os

import numpy as np
import pandas as pd
from scipy import stats

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# grid, method name, ensemble label, base learner label, reference configuration of the base learner
# (FIMT-DD: SGBR's default base learner; BRIM: the default of the single trees)
FIMTDD_REFERENCE = "trees.FIMTDD -g 50 -c 0.01 -e"
BRIM_REFERENCE = "trees.BRIMRegressor -g 200 -c 0.0000001 -R 200 -k 5 -q 0.25 -D 20 -B 5 -V"
GRIDS = [("oza_fimtdd", "OzaBag-FIMTDD", "OzaBag", "FIMT-DD", FIMTDD_REFERENCE),
         ("oza_brim", "OzaBag-BRIM", "OzaBag", "BRIM", BRIM_REFERENCE),
         ("sgbr_fimtdd", "SGBR-FIMTDD", "SGBR", "FIMT-DD", FIMTDD_REFERENCE),
         ("sgbr_brim", "SGBR-BRIM", "SGBR", "BRIM", BRIM_REFERENCE)]
# Variants of the selected BRIM ensembles: suffix and the change to each BRIM member (memory budgets in bytes, and
# no re-evaluation after a drift)
BRIM_VARIANTS = [("1MB", lambda b: f"{b} -m 1000000"), ("100KB", lambda b: f"{b} -m 100000"),
                 ("noV", lambda b: b.replace(" -V", ""))]


def base_learner(cli):
    """The base learner CLI inside an ensemble CLI (the innermost parentheses)."""
    return cli[cli.rindex("(") + 1:cli.index(")")]


def latex_escape(s):
    return s.replace("_", r"\_").replace("%", r"\%")


def main():
    summary_dir = os.path.join(EXP_DIR, "results", "summary")
    os.makedirs(summary_dir, exist_ok=True)
    datasets = [l.strip() for l in open(os.path.join(EXP_DIR, "configs", "ensemble_tuning_datasets.txt"))
                if l.strip() and not l.startswith("#")]
    selected = []
    for key, name, ensemble, base, reference in GRIDS:
        files = glob.glob(os.path.join(EXP_DIR, "results", "raw", "ensemble_tuning", f"{key}_*.csv"))
        raw = pd.concat([pd.read_csv(f) for f in files], ignore_index=True)
        raw = raw[raw["dataset"].isin(datasets)]
        raw["adj_r2"] = pd.to_numeric(raw["adj_r2"], errors="coerce").fillna(-np.inf)  # NaN: diverged or timed out
        grid = {l.split("\t")[0]: l.rstrip("\n").split("\t")[1]
                for l in open(os.path.join(EXP_DIR, "configs", "ensemble_tuning", f"{key}.tsv"))
                if l.strip() and not l.startswith("#")}
        # A run without a result (it failed, e.g., ran out of memory) also counts as -infinity; the selection runs once
        # the tuning has finished, so every run of the grid has been attempted
        seeds = sorted(raw["seed"].unique())
        index = pd.MultiIndex.from_product([datasets, list(grid), seeds],
                                           names=["dataset", "config", "seed"])
        runs = raw.groupby(["dataset", "config", "seed"])["adj_r2"].mean().reindex(index)
        if runs.isna().any():
            print(f"{name}: {runs.isna().sum()} runs without a result, counted as diverged")
        means = runs.fillna(-np.inf).groupby(["dataset", "config"]).mean().unstack("config")
        complete = means
        ranks = complete.apply(lambda row: pd.Series(stats.rankdata(-row.values, method="average"), index=row.index), axis=1)
        table = pd.DataFrame({"avg_rank": ranks.mean(), "median_adj_r2": complete.median(),
                              "num_datasets": len(complete)})
        table["cli"] = [grid[c] for c in table.index]
        table = table.sort_values(["avg_rank", "median_adj_r2"], ascending=[True, False])
        table.to_csv(os.path.join(summary_dir, f"ensemble_tuning_{key}.csv"))
        best = table.index[0]
        ref = next(c for c in table.index if base_learner(grid[c]) == reference)
        print(f"{name}: best {best} (avg rank {table.loc[best, 'avg_rank']:.2f}, median adj R2 "
              f"{table.loc[best, 'median_adj_r2']:.3f}); reference {ref} (avg rank {table.loc[ref, 'avg_rank']:.2f}, "
              f"position {list(table.index).index(ref) + 1} of {len(table)}); {len(complete)} datasets")
        selected.append((name, ensemble, base, grid[best], len(table), table.loc[best, "avg_rank"],
                         table.loc[ref, "avg_rank"]))

    with open(os.path.join(EXP_DIR, "configs", "ensemble_methods.tsv"), "w") as f:
        f.write("# name<TAB>MOA learner CLI (base learners selected by analysis/select_ensembles.py; the -1MB and -100KB "
                "variants limit each BRIM member to 1,000,000 and 100,000 bytes, and the -noV variants do not "
                "re-evaluate the splits after a drift)\n")
        for name, _, base, cli, *_ in selected:
            f.write(f"{name}\t{cli}\n")
            if base == "BRIM":
                inner = base_learner(cli)
                for suffix, change in BRIM_VARIANTS:
                    f.write(f"{name}-{suffix}\t{cli.replace(inner, change(inner))}\n")

    lines = [r"\begin{tabular}{lllrrr}", r"\toprule",
             r"Ensemble & Base learner & Selected base learner (MOA options) & Configurations & "
             r"\multicolumn{2}{c}{Avg.\ rank} \\",
             r"\cmidrule(lr){5-6} & & & & selected & reference \\", r"\midrule"]
    for _, ensemble, base, cli, n, rank_best, rank_ref in selected:
        inner = base_learner(cli)
        options = inner.split(" ", 1)[1] if " " in inner else "(defaults)"
        count = f"{n:,}".replace(",", "{,}")
        lines.append(f"{ensemble} & {base} & \\texttt{{{latex_escape(options)}}} & {count} & {rank_best:.2f} & "
                     f"{rank_ref:.2f} \\\\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    os.makedirs(os.path.join(EXP_DIR, "paper", "tables"), exist_ok=True)
    with open(os.path.join(EXP_DIR, "paper", "tables", "ensemble_tuning.tex"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print("wrote configs/ensemble_methods.tsv and paper/tables/ensemble_tuning.tex")


if __name__ == "__main__":
    main()
