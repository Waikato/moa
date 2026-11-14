#!/usr/bin/env python3
"""Selects the base learner configuration of SGBT (StreamingGradientBoostedTrees) for classification from the tuning
runs (results/raw/sgbt_tuning/parts/*.csv, written by scripts/run_sgbt_tuning.sh, or, without them, their merge
results/raw/sgbt_tuning.csv, which is the version kept in git) on the streams of configs/sgbt_tuning_datasets.txt
(CDatasets_tuning).

SGBT keeps its default structure (100 boosting steps, learning rate 0.0125, 75% of the attributes per step) and only the
base learner (FIMT-DD with one-hot encoded nominal attributes, or BRIM with native nominal attributes, -H) is tuned. As
in select_ensembles.py, the configurations are ranked on each dataset by their accuracy (mean over the seeds) and the
configuration with the best average rank is selected (ties: higher median accuracy). A run that diverged (non-finite
accuracy or votes) or failed (no result, e.g., out of memory) counts as -inf, so its configuration ranks last on that
dataset instead of removing the dataset.

Besides the best BRIM configuration, two fast ones are kept (FAST_VARIANTS): the fastest BRIM configuration (lowest
total wall time over the tuning streams; a failed run counts as infinite) and the fastest of the best 10% by average
rank.

Writes results/summary/sgbt_tuning_<grid>.csv, configs/sgbt_methods.tsv (the tuned methods, the fast BRIM
configurations, the best BRIM with budgets of 1 MB, 500 KB and 100 KB per tree, and SGBT with its default base learner as
a reference) and paper/tables/sgbt_tuning.tex. The final BRIM methods add FINAL_OPTIONS to their configuration: mean
leaves (-e), which inside SGBT were about a quarter faster than the adaptive leaves used in the tuning at the same or
better accuracy in a preliminary speed probe.
"""
import glob
import os

import numpy as np
import pandas as pd
from scipy import stats

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# grid, method name, base learner label, reference configuration of the base learner (SGBT's default for FIMT-DD)
GRIDS = [("sgbt_fimtdd", "SGBT-FIMTDD", "FIMT-DD", "trees.FIMTDD -s VarianceReductionSplitCriterion -g 25 -c 0.05 -e -p"),
         ("sgbt_brim", "SGBT-BRIM", "BRIM", "trees.BRIMRegressor")]
# Memory budgets of each BRIM tree of the budgeted variants
MEMBER_BUDGETS = [("1MB", 1000000), ("500KB", 500000), ("100KB", 100000)]
DEFAULT = ("SGBT-default", "meta.StreamingGradientBoostedTrees")
# Options added to the selected configurations of a base learner in the final runs (not tuned)
FINAL_OPTIONS = {"BRIM": "-e"}
# Fast BRIM configurations of the final runs: method name suffix, and the share of the configurations, by average rank,
# among which the fastest is taken (1: all)
FAST_VARIANTS = [("fastgood", 0.1), ("fast", 1.0)]


def base_learner(cli):
    """The base learner CLI inside an SGBT CLI (the parentheses of -l)."""
    return cli[cli.index("(") + 1:cli.rindex(")")]


def latex_escape(s):
    return s.replace("_", r"\_").replace("%", r"\%")


def main():
    summary_dir = os.path.join(EXP_DIR, "results", "summary")
    os.makedirs(summary_dir, exist_ok=True)
    os.makedirs(os.path.join(EXP_DIR, "paper", "tables"), exist_ok=True)
    datasets = [l.strip() for l in open(os.path.join(EXP_DIR, "configs", "sgbt_tuning_datasets.txt"))
                if l.strip() and not l.startswith("#")]
    selected = []
    fast_methods = []  # (method name, CLI) of the fast BRIM configurations
    for key, name, base, reference in GRIDS:
        grid = {l.split("\t")[0]: l.rstrip("\n").split("\t")[1]
                for l in open(os.path.join(EXP_DIR, "configs", "sgbt_tuning", f"{key}.tsv"))
                if l.strip() and not l.startswith("#")}
        files = glob.glob(os.path.join(EXP_DIR, "results", "raw", "sgbt_tuning", "parts", "*.csv"))
        raw = (pd.concat([pd.read_csv(f) for f in files], ignore_index=True) if files
               else pd.read_csv(os.path.join(EXP_DIR, "results", "raw", "sgbt_tuning.csv")))
        raw = raw[raw["dataset"].isin(datasets) & raw["config"].isin(grid)]
        accuracy = pd.to_numeric(raw["accuracy"], errors="coerce")
        raw["accuracy"] = accuracy.where(np.isfinite(accuracy) & (raw["nonfinite_votes"] == 0), -np.inf)
        means = raw.groupby(["dataset", "config"])["accuracy"].mean().unstack("config")
        means = means.reindex(index=[d for d in datasets if d in means.index], columns=list(grid))
        # Runs that are missing after the tuning failed (e.g., out of memory): they rank last, as diverged runs
        missing = means.isna().stack()
        for dataset, config in missing[missing].index:
            print(f"  warning: no result for {config} on {dataset} (failed run?), ranked last")
        complete = means.fillna(-np.inf)
        ranks = complete.apply(lambda row: pd.Series(stats.rankdata(-row.values, method="average"), index=row.index), axis=1)
        table = pd.DataFrame({"avg_rank": ranks.mean(), "median_accuracy": complete.median(),
                              "num_datasets": len(complete)})
        table["cli"] = [grid[c] for c in table.index]
        table = table.sort_values(["avg_rank", "median_accuracy"], ascending=[True, False])
        table.to_csv(os.path.join(summary_dir, f"sgbt_tuning_{key}.csv"))
        best = table.index[0]
        ref = next((c for c in table.index if base_learner(grid[c]) == reference), None)
        ref_text = (f"reference {ref} (avg rank {table.loc[ref, 'avg_rank']:.2f}, position "
                    f"{list(table.index).index(ref) + 1} of {len(table)})" if ref else "reference not in the grid")
        print(f"{name}: best {best} (avg rank {table.loc[best, 'avg_rank']:.2f}, median accuracy "
              f"{table.loc[best, 'median_accuracy']:.2f}); {ref_text}; {len(complete)} of {len(datasets)} datasets")
        selected.append((name, base, grid[best], len(table), table.loc[best, "avg_rank"],
                         table.loc[ref, "avg_rank"] if ref else float("nan")))
        if base == "BRIM":
            times = raw.groupby(["dataset", "config"])["learner_wall_s"].mean().unstack("config")
            table["total_wall_s"] = times.reindex(index=complete.index, columns=table.index).fillna(np.inf).sum()
            table.to_csv(os.path.join(summary_dir, f"sgbt_tuning_{key}.csv"))
            for label, share in FAST_VARIANTS:
                pool = table[table["avg_rank"] <= table["avg_rank"].quantile(share)]
                fast = pool["total_wall_s"].idxmin()
                print(f"{name}-{label}: {fast} (avg rank {table.loc[fast, 'avg_rank']:.2f}, total wall time "
                      f"{table.loc[fast, 'total_wall_s']:.0f} s vs. {table.loc[best, 'total_wall_s']:.0f} s of the best)")
                fast_methods.append((f"{name}-{label}", grid[fast]))

    with open(os.path.join(EXP_DIR, "configs", "sgbt_methods.tsv"), "w") as f:
        f.write("# name<TAB>MOA learner CLI (base learners selected by analysis/select_sgbt.py; "
                "BRIM adds mean leaves (-e) to its tuned configurations; -fastgood and -fast are the fastest BRIM configurations "
                "of the best 10% by average rank and of all; "
                "the -1MB, -500KB and -100KB variants limit each BRIM tree to that budget (-m, bytes); "
                f"{DEFAULT[0]} is SGBT with its default base learner)\n")
        def final_cli(base, cli):
            inner = base_learner(cli)
            return cli.replace(inner, f"{inner} {FINAL_OPTIONS[base]}") if base in FINAL_OPTIONS else cli
        for name, base, cli, *_ in selected:
            cli = final_cli(base, cli)
            f.write(f"{name}\t{cli}\n")
            if base == "BRIM":
                inner = base_learner(cli)
                for label, budget in MEMBER_BUDGETS:
                    f.write(f"{name}-{label}\t{cli.replace(inner, f'{inner} -m {budget}')}\n")
                for fast_name, fast_cli in fast_methods:
                    f.write(f"{fast_name}\t{final_cli(base, fast_cli)}\n")
        # The reference is left out when tuning selects SGBT's default base learner (the runs would be identical)
        fimtdd_cli = next(cli for name, base, cli, *_ in selected if base == "FIMT-DD")
        if base_learner(fimtdd_cli) != GRIDS[0][3]:
            f.write(f"{DEFAULT[0]}\t{DEFAULT[1]}\n")
        else:
            print(f"{DEFAULT[0]} left out: the selected FIMT-DD configuration is SGBT's default")

    lines = [r"\begin{tabular}{llrrr}", r"\toprule",
             r"Base learner & Selected configuration (MOA options) & Configurations & "
             r"\multicolumn{2}{c}{Avg.\ rank} \\",
             r"\cmidrule(lr){4-5} & & & selected & SGBT default \\", r"\midrule"]
    for _, base, cli, n, rank_best, rank_ref in selected:
        inner = base_learner(cli)
        options = inner.split(" ", 1)[1] if " " in inner else "(defaults)"
        ref_cell = "--" if np.isnan(rank_ref) else f"{rank_ref:.2f}"
        lines.append(f"{base} & \\texttt{{{latex_escape(options)}}} & {n} & {rank_best:.2f} & {ref_cell} \\\\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", "sgbt_tuning.tex"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print("wrote configs/sgbt_methods.tsv and paper/tables/sgbt_tuning.tex")


if __name__ == "__main__":
    main()
