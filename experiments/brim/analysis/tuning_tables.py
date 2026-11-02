#!/usr/bin/env python3
"""Per-dataset tables of the single-tree tuning (results/raw/tuning/*.csv) on the 10 tuning streams: the adjusted R^2 and
the learner wall time (means over seeds 1-3) of one configuration per method and dataset, each with the rank of the
method on that dataset (adjusted R^2: higher is better; time: lower is better) and the average ranks. Three choices of
the configuration:

  best_r2    the configuration with the highest adjusted R^2 on that dataset
  best_time  the configuration with the shortest wall time on that dataset
  selected   the configuration selected by analysis/select_baselines.py (results/summary/tuning_<method>.csv)

The best_* tables pick the configuration on the dataset that is reported, so they are optimistic, the more so the larger
the grid (BRIM: 2187 configurations, the baselines: 18 to 27). Tuning ran 16 runs at a time, so the wall times are
noisier and longer than in the main experiments.

Writes paper/tables/tuning_<choice>.tex (tabular only) and results/summary/tuning_<choice>.csv (with the configurations).
"""
import glob
import os

import pandas as pd
from scipy import stats

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# key of the tuning files, name of the configurations (prefix), label in the tables; in the order of the main tables
METHODS = [("fimtdd", "FIMTDD", "FIMT-DD"), ("orto", "ORTO", "ORTO"), ("amrules", "AMRules", "AMRules"),
           ("sgt", "SGT", "SGT"), ("brim", "BRIM", r"\brim")]


def ranks(values, higher_is_better):
    return values.apply(lambda row: pd.Series(stats.rankdata(-row.values if higher_is_better else row.values),
                                              index=row.index), axis=1)


def fmt_r2(v):
    return f"{v:.3f}" if abs(v) < 10 else (r"$<\!-10$" if v < 0 else r"$>\!10$")


def cell(text, rank):
    text = rf"\textbf{{{text}}}" if rank == 1 else text
    return rf"{text}\,{{\scriptsize({rank:g})}}"


def latex(r2, time, r2_ranks, time_ranks, path):
    names = [label for _, name, label in METHODS]
    lines = [r"\begin{tabular}{l" + "rr" * len(names) + "}", r"\toprule",
             "Dataset & " + " & ".join(rf"\multicolumn{{2}}{{c}}{{{n}}}" for n in names) + r" \\",
             " ".join(rf"\cmidrule(lr){{{2 + 2 * i}-{3 + 2 * i}}}" for i in range(len(names))),
             " & " + " & ".join([r"$\bar R^2$ & Time (s)"] * len(names)) + r" \\", r"\midrule"]
    for d in r2.index:
        cells = []
        for _, name, _ in METHODS:
            cells += [cell(fmt_r2(r2.loc[d, name]), r2_ranks.loc[d, name]),
                      cell(f"{time.loc[d, name]:.2f}", time_ranks.loc[d, name])]
        lines.append(f"{d} & " + " & ".join(cells) + r" \\")
    lines.append(r"\midrule")
    avg = []
    best_r2, best_time = r2_ranks.mean().min(), time_ranks.mean().min()
    for _, name, _ in METHODS:
        for value, best in [(r2_ranks[name].mean(), best_r2), (time_ranks[name].mean(), best_time)]:
            avg.append(rf"\textbf{{{value:.2f}}}" if value == best else f"{value:.2f}")
    lines += ["Avg.\\ rank & " + " & ".join(avg) + r" \\", r"\bottomrule", r"\end{tabular}"]
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


def main():
    raw = pd.concat([pd.read_csv(f) for f in glob.glob(os.path.join(EXP_DIR, "results", "raw", "tuning", "*.csv"))],
                    ignore_index=True)
    raw["method"] = raw.config.str.split("_").str[0]
    means = raw.groupby(["method", "dataset", "config"])[["adj_r2", "learner_wall_s"]].mean().reset_index()
    selected = {name: pd.read_csv(os.path.join(EXP_DIR, "results", "summary", f"tuning_{key}.csv")).iloc[0, 0]
                for key, name, _ in METHODS}
    choices = {
        "best_r2": means.loc[means.groupby(["method", "dataset"]).adj_r2.idxmax()],
        "best_time": means.loc[means.groupby(["method", "dataset"]).learner_wall_s.idxmin()],
        "selected": means[means.config.isin(selected.values())],
    }
    order = [name for _, name, _ in METHODS]
    for choice, rows in choices.items():
        r2 = rows.pivot(index="dataset", columns="method", values="adj_r2")[order]
        time = rows.pivot(index="dataset", columns="method", values="learner_wall_s")[order]
        config = rows.pivot(index="dataset", columns="method", values="config")[order]
        r2_ranks, time_ranks = ranks(r2, True), ranks(time, False)
        latex(r2, time, r2_ranks, time_ranks, os.path.join(EXP_DIR, "paper", "tables", f"tuning_{choice}.tex"))
        pd.concat({"adj_r2": r2, "adj_r2_rank": r2_ranks, "time_s": time, "time_rank": time_ranks, "config": config},
                  axis=1).to_csv(os.path.join(EXP_DIR, "results", "summary", f"tuning_{choice}.csv"))
        print(f"{choice}: avg rank adj R2 " + ", ".join(f"{m} {r2_ranks[m].mean():.2f}" for m in order)
              + "; time " + ", ".join(f"{m} {time_ranks[m].mean():.2f}" for m in order))
    print("wrote paper/tables/tuning_{best_r2,best_time,selected}.tex and results/summary/tuning_{...}.csv")


if __name__ == "__main__":
    main()
