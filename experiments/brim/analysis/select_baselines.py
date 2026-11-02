#!/usr/bin/env python3
"""Selects the best configuration of BRIM and of each baseline from the tuning runs (results/raw/tuning/*.csv, written
by scripts/run_tuning.sh) on the 10 streams of configs/tuning_datasets.txt (the ablation streams without hyperA).

For each method, the configurations are ranked on each dataset by their adjusted R^2 (mean over seeds 1-3, rank 1 is
the best); the configuration with the best average rank is selected (ties: higher median adjusted R^2). Ranks are used
instead of mean values since some configurations diverge (adjusted R^2 of -10^20 and lower); a non-finite adjusted R^2
counts as -inf, so its configuration ranks last on that dataset. Datasets on which a configuration has no run (an
unfinished tuning) are left out.

For BRIM, the fastest configuration is also selected, in the same way by the learner wall time (rank 1 is the shortest;
ties: higher median adjusted R^2), as BRIM-fast. Tuning runs in parallel, so these times are noisy.

Writes results/summary/tuning_<method>.csv (all configurations, sorted), configs/main_methods.tsv (the baselines and
BRIM with their selected configurations, BRIM-fast, and both BRIMs with budgets of 1 MB and 100 KB),
configs/ablation_variants.tsv (the selected BRIM with one component removed at a time) and paper/tables/tuning.tex.
"""
import glob
import os

import numpy as np
import pandas as pd
from scipy import stats

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# key, name, label, default configuration (the reference in the tuning table)
METHODS = [("brim", "BRIM", "BRIM", "trees.BRIMRegressor -g 200 -c 0.0000001 -R 200 -k 5 -q 0.25 -D 20 -B 5 -V"),
           ("fimtdd", "FIMTDD", "FIMT-DD", "trees.FIMTDD -g 200 -c 0.0000001"),
           ("orto", "ORTO", "ORTO", "trees.ORTO -g 200 -o average"),
           ("amrules", "AMRules", "AMRules", "rules.AMRulesRegressor -g 200 -L moa.classifiers.rules.multilabel.functions.AdaptiveMultiTargetRegressor"),
           ("sgt", "SGT", "SGT", "trees.StochasticGradientTree -G 200 -C 0.000001 -W 1000")]
# Budgeted variants of BRIM and BRIM-fast in the main experiments: the selected configuration with a memory budget
# (-m, bytes), named <name>-<suffix>
BRIM_BUDGETS = [("1MB", 1000000), ("100KB", 100000)]
# Ablation variants: options that replace (or add to) those of the selected BRIM; None marks a flag and False removes
# an option
ABLATION = [("BRIM", {}), ("no-QO", {"-O": None}), ("no-adaptive-leaf", {"-N": None}), ("mean-leaf", {"-e": None}),
            ("no-clipping", {"-C": None}), ("no-drift-detection", {"-u": None}),
            ("no-guarded-replacement", {"-I": None}), ("no-drift-reevaluation", {"-V": False}),
            ("no-attribute-subset", {"-k": "0"}), ("no-restructuring", {"-B": "1"}),
            ("no-reevaluation", {"-R": "2000000000", "-V": False}), ("multiway-nominal", {"-M": None})]


def latex_escape(s):
    return s.replace("_", r"\_").replace("%", r"\%")


def with_options(cli, options):
    """The CLI with the given options set: a valued option replaces the one of the CLI, a flag (None) is added and
    an option set to False is removed."""
    learner, *tokens = cli.split()
    values = {}
    i = 0
    while i < len(tokens):  # an option followed by a token that is not an option takes it as its value
        if i + 1 < len(tokens) and not tokens[i + 1].startswith("-"):
            values[tokens[i]] = tokens[i + 1]
            i += 2
        else:
            values[tokens[i]] = None
            i += 1
    values.update(options)
    return " ".join([learner] + [t for o, v in values.items() if v is not False for t in ([o] if v is None else [o, v])])


def main():
    summary_dir = os.path.join(EXP_DIR, "results", "summary")
    os.makedirs(summary_dir, exist_ok=True)
    selected = {}
    for key, name, label, default_cli in METHODS:
        files = glob.glob(os.path.join(EXP_DIR, "results", "raw", "tuning", f"{key}_*.csv"))
        raw = pd.concat([pd.read_csv(f) for f in files], ignore_index=True)
        grid = {l.split("\t")[0]: l.rstrip("\n").split("\t")[1]
                for l in open(os.path.join(EXP_DIR, "configs", "tuning", f"{key}.tsv")) if l.strip() and not l.startswith("#")}
        means = raw.groupby(["dataset", "config"])["adj_r2"].mean().unstack("config").reindex(columns=list(grid))
        has_runs = raw.groupby(["dataset", "config"]).size().unstack("config").reindex(columns=list(grid)).notna()
        complete = means[has_runs.all(axis=1)]
        if len(complete) < len(means):
            print(f"{name}: left out {sorted(set(means.index) - set(complete.index))} (configurations without runs)")
        complete = complete.replace([np.inf, -np.inf], np.nan).fillna(-np.inf)
        ranks = complete.apply(lambda row: pd.Series(stats.rankdata(-row.values, method="average"), index=row.index), axis=1)
        table = pd.DataFrame({"avg_rank": ranks.mean(), "median_adj_r2": complete.median(),
                              "num_datasets": len(complete)})
        table["cli"] = [grid[c] for c in table.index]
        table = table.sort_values(["avg_rank", "median_adj_r2"], ascending=[True, False])
        table.to_csv(os.path.join(summary_dir, f"tuning_{key}.csv"))
        best = table.index[0]
        default = next(c for c in table.index if grid[c] == default_cli)
        print(f"{name}: best {best} (avg rank {table.loc[best, 'avg_rank']:.2f}, median adj R2 "
              f"{table.loc[best, 'median_adj_r2']:.3f}); default {default} (avg rank {table.loc[default, 'avg_rank']:.2f}, "
              f"position {list(table.index).index(default) + 1} of {len(table)}); {len(complete)} datasets")
        selected[key] = (name, label, grid[best], len(table), table.loc[best, "avg_rank"], table.loc[default, "avg_rank"],
                         list(table.index).index(default) + 1)
        if key == "brim":
            times = raw.groupby(["dataset", "config"])["learner_wall_s"].mean().unstack("config")
            times = times.reindex(columns=list(grid)).loc[complete.index].fillna(np.inf)
            time_ranks = times.apply(lambda row: pd.Series(stats.rankdata(row.values, method="average"), index=row.index),
                                     axis=1)
            fast_table = pd.DataFrame({"avg_time_rank": time_ranks.mean(), "mean_time_s": times.mean(),
                                       "median_adj_r2": complete.median(), "avg_rank": ranks.mean()})
            fast_table = fast_table.sort_values(["avg_time_rank", "median_adj_r2"], ascending=[True, False])
            fast_table.to_csv(os.path.join(summary_dir, "tuning_brim_time.csv"))
            fast = fast_table.index[0]
            brim_fast = grid[fast]
            print(f"BRIM-fast: {fast} (avg time rank {fast_table.loc[fast, 'avg_time_rank']:.1f}, mean time "
                  f"{fast_table.loc[fast, 'mean_time_s']:.2f} s, avg rank in adjusted R2 {fast_table.loc[fast, 'avg_rank']:.1f})")

    brim = selected["brim"][2]
    with open(os.path.join(EXP_DIR, "configs", "main_methods.tsv"), "w") as f:
        f.write("# name<TAB>MOA learner CLI (baselines and BRIM with the configurations selected by "
                "analysis/select_baselines.py; the budgeted BRIM variants add -m)\n")
        for key in ["fimtdd", "orto", "amrules", "sgt", "brim"]:
            f.write(f"{selected[key][0]}\t{selected[key][2]}\n")
        for suffix, budget in BRIM_BUDGETS:
            f.write(f"BRIM-{suffix}\t{with_options(brim, {'-m': str(budget)})}\n")
        f.write(f"BRIM-fast\t{brim_fast}\n")
        for suffix, budget in BRIM_BUDGETS:
            f.write(f"BRIM-fast-{suffix}\t{with_options(brim_fast, {'-m': str(budget)})}\n")
    with open(os.path.join(EXP_DIR, "configs", "ablation_variants.tsv"), "w") as f:
        f.write("# name<TAB>MOA learner CLI (the selected BRIM with one component removed; written by "
                "analysis/select_baselines.py)\n")
        for name, options in ABLATION:
            f.write(f"{name}\t{with_options(brim, options)}\n")

    # Table of the methods of the main experiments, in the order of configs/main_methods.tsv
    lines = [r"\begin{tabular}{llrrrr}", r"\toprule",
             r"Method & Selected configuration (MOA options) & Configurations & \multicolumn{2}{c}{Avg.\ rank} & Position \\",
             r"\cmidrule(lr){4-5} & & & selected & default & of default \\", r"\midrule"]
    for key in ["fimtdd", "orto", "amrules", "sgt", "brim"]:
        _, label, cli, n, rank_best, rank_default, position = selected[key]
        options = cli.split(" ", 1)[1] if " " in cli else "(defaults)"
        options = options.replace("moa.classifiers.rules.multilabel.functions.", "")
        label = r"\brim" if key == "brim" else label
        lines.append(f"{label} & \\texttt{{{latex_escape(options)}}} & {n:,} & {rank_best:.2f} & {rank_default:.2f} & "
                     f"{position:,} \\\\".replace(",", "{,}"))
    for suffix, budget in BRIM_BUDGETS:
        lines.append(f"\\brim{{}}-{suffix} & as \\brim, with \\texttt{{-m {budget}}} & & & & \\\\")
    fast_options = latex_escape(brim_fast.split(" ", 1)[1])
    lines.append(f"\\brim{{}}-fast & \\texttt{{{fast_options}}} & {selected['brim'][3]:,} & "
                 f"{fast_table.loc[fast, 'avg_rank']:.2f} & & \\\\".replace(",", "{,}", 1))
    for suffix, budget in BRIM_BUDGETS:
        lines.append(f"\\brim{{}}-fast-{suffix} & as \\brim-fast, with \\texttt{{-m {budget}}} & & & & \\\\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", "tuning.tex"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print("wrote configs/main_methods.tsv, configs/ablation_variants.tsv and paper/tables/tuning.tex")


if __name__ == "__main__":
    main()
