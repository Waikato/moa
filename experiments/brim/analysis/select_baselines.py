#!/usr/bin/env python3
"""Selects the best configuration of each baseline from the tuning runs (results/raw/tuning/*.csv, written by
scripts/run_tuning.sh) on the 11 small streams of the ablation study.

For each method, the configurations are ranked on each dataset by their adjusted R^2 (mean over seeds 1-3, rank 1 is
the best); the configuration with the best average rank is selected (ties: higher median adjusted R^2). Ranks are used
instead of mean values since some configurations diverge (adjusted R^2 of -10^20 and lower).

Writes results/summary/tuning_<method>.csv (all configurations, sorted), configs/main_methods.tsv (the baselines with
their selected configurations, followed by the BRIM variants) and paper/tables/tuning.tex.
"""
import glob
import os

import numpy as np
import pandas as pd
from scipy import stats

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
METHODS = [("fimtdd", "FIMTDD", "FIMT-DD"), ("orto", "ORTO", "ORTO"), ("amrules", "AMRules", "AMRules"),
           ("sgt", "SGT", "SGT")]
BRIM_ROWS = [("BRIM", "trees.BRIMRegressor"), ("BRIM-1MB", "trees.BRIMRegressor -m 1000000"),
             ("BRIM-100KB", "trees.BRIMRegressor -m 100000")]


def latex_escape(s):
    return s.replace("_", r"\_").replace("%", r"\%")


def main():
    summary_dir = os.path.join(EXP_DIR, "results", "summary")
    os.makedirs(summary_dir, exist_ok=True)
    selected = []
    for key, name, label in METHODS:
        files = glob.glob(os.path.join(EXP_DIR, "results", "raw", "tuning", f"{key}_*.csv"))
        raw = pd.concat([pd.read_csv(f) for f in files], ignore_index=True)
        grid = {l.split("\t")[0]: l.rstrip("\n").split("\t")[1]
                for l in open(os.path.join(EXP_DIR, "configs", "tuning", f"{key}.tsv")) if l.strip() and not l.startswith("#")}
        means = raw.groupby(["dataset", "config"])["adj_r2"].mean().unstack("config")
        complete = means.dropna()  # datasets with all configurations
        ranks = complete.apply(lambda row: pd.Series(stats.rankdata(-row.values, method="average"), index=row.index), axis=1)
        table = pd.DataFrame({"avg_rank": ranks.mean(), "median_adj_r2": complete.median(),
                              "num_datasets": len(complete)})
        table["cli"] = [grid[c] for c in table.index]
        table = table.sort_values(["avg_rank", "median_adj_r2"], ascending=[True, False])
        table.to_csv(os.path.join(summary_dir, f"tuning_{key}.csv"))
        best = table.index[0]
        default = next(c for c in table.index if grid[c] == {"fimtdd": "trees.FIMTDD -g 200 -c 0.0000001",
                                                              "orto": "trees.ORTO -g 200 -o average",
                                                              "amrules": "rules.AMRulesRegressor -g 200 -L moa.classifiers.rules.multilabel.functions.AdaptiveMultiTargetRegressor",
                                                              "sgt": "trees.StochasticGradientTree -G 200 -C 0.000001 -W 1000"}[key])
        print(f"{name}: best {best} (avg rank {table.loc[best, 'avg_rank']:.2f}, median adj R2 "
              f"{table.loc[best, 'median_adj_r2']:.3f}); default {default} (avg rank {table.loc[default, 'avg_rank']:.2f}, "
              f"position {list(table.index).index(default) + 1} of {len(table)}); {len(complete)} datasets")
        selected.append((name, label, grid[best], len(table), table.loc[best, "avg_rank"], table.loc[default, "avg_rank"]))

    with open(os.path.join(EXP_DIR, "configs", "main_methods.tsv"), "w") as f:
        f.write("# name<TAB>MOA learner CLI (baselines with the configurations selected by analysis/select_baselines.py)\n")
        for name, _, cli, *_ in selected:
            f.write(f"{name}\t{cli}\n")
        for name, cli in BRIM_ROWS:
            f.write(f"{name}\t{cli}\n")

    lines = [r"\begin{tabular}{llrrr}", r"\toprule",
             r"Method & Selected configuration (MOA options) & Configurations & \multicolumn{2}{c}{Avg.\ rank} \\",
             r"\cmidrule(lr){4-5} & & & selected & default \\", r"\midrule"]
    for _, label, cli, n, rank_best, rank_default in selected:
        options = cli.split(" ", 1)[1] if " " in cli else "(defaults)"
        options = options.replace("moa.classifiers.rules.multilabel.functions.", "")
        lines.append(f"{label} & \\texttt{{{latex_escape(options)}}} & {n} & {rank_best:.2f} & {rank_default:.2f} \\\\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", "tuning.tex"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print("wrote configs/main_methods.tsv and paper/tables/tuning.tex")


if __name__ == "__main__":
    main()
