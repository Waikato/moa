#!/usr/bin/env python3
"""Analysis of the SGBT classification experiment (BRIM instead of FIMT-DD as the base learner of
StreamingGradientBoostedTrees): the selected configurations on the tuning streams and the results of the final runs.

Usage: python3 analysis/analyze_sgbt.py [--alpha 0.05]

Reads results/raw/sgbt_tuning.csv with results/summary/sgbt_tuning_*.csv (written by select_sgbt.py), and
results/raw/sgbt.csv (written by scripts/run_sgbt_final.sh) with the methods of configs/sgbt_methods.tsv and the
streams of configs/sgbt_datasets.txt, and writes
  paper/tables/sgbt_tuning_streams.tex    accuracy and wall time of the selected configurations on the tuning streams
  paper/tables/sgbt_final.tex             accuracy, final memory and wall time of the final runs per stream (mean +- sd
                                          over the seeds) with the average ranks
  paper/tables/sgbt_avg_ranks.tex         average ranks and Friedman p-values
  paper/figures/cd_sgbt_*.pdf|png         CD diagrams (Friedman test and Shaffer's post-hoc test)
  results/summary/sgbt_final.csv          per stream and method: seeds completed, stopped and failed, means and sds
  results/summary/sgbt_final_status.csv   status of every run
  results/summary/sgbt_*_ranks.csv, sgbt_*_shaffer.csv, sgbt_avg_ranks.csv

A run is completed, stopped at the time limit (recorded with empty measures, "TO"), or failed (no result line; its log
results/logs/<dataset>.<method>.<seed>.err tells why: "OOM" for an OutOfMemoryError of the 30 GB heap, "killed" for a
death by signal, e.g., the Linux OOM killer, "fail" otherwise). The means are over the completed seeds; a method
without a completed seed on a stream has no value there and ranks last on it in all measures (ties: average rank), as
in the tuning: within the time and memory limits, it produced no model.
"""
import argparse
import glob
import os
import re
import sys

import numpy as np
import pandas as pd

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from analyze import cd_diagram, friedman, rank_matrix, shaffer_posthoc  # noqa: E402

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FINAL_MEASURES = [  # column, label, scale, higher is better, short name
    ("accuracy", r"Accuracy (\%)", 1, True, "accuracy"),
    ("mem_final_bytes", "Final memory (MB)", 1e-6, False, "memory"),
    ("learner_wall_s", "Wall time (s)", 1, False, "time"),
]
SHORT_LABELS = {"timeout": "TO", "oom": "OOM", "killed": "killed", "failed": "fail", "missing": "--"}


def decimals(column, mean):
    """Accuracy with two decimals; memory and time (several orders of magnitude) with two below 10, one below 100, none
    above."""
    return 2 if column == "accuracy" or abs(mean) < 10 else 1 if abs(mean) < 100 else 0


def fmt(v, d):
    return f"{v:,.{d}f}".replace(",", "{,}")


def label(method):
    return method.replace("SGBT-", "").replace("FIMTDD", "FIMT-DD").replace("BRIM", r"\brim")


def tex(name):
    return name.replace("_", r"\_")


def read_list(path):
    return [l.split("\t")[0].strip() for l in open(path) if l.strip() and not l.startswith("#")]


def failure(log_dir, dataset, method, seed):
    """Why a run without a result line failed, from its log (see run_sgbt.sh); "missing" without a log."""
    path = os.path.join(log_dir, f"{dataset}.{method}.{seed}.err")
    if not os.path.exists(path):
        return "missing"
    text = open(path, errors="replace").read()
    if "OutOfMemoryError" in text:
        return "oom"
    codes = re.findall(r"exit code (\d+)", text)
    return "killed" if codes and int(codes[-1]) > 128 else "failed"


def run_status(raw, datasets, methods, seeds):
    """Status of every (dataset, method, seed): ok, timeout, oom, killed, failed or missing."""
    log_dir = os.path.join(EXP_DIR, "results", "logs")
    recorded = {(r.dataset, r.config, int(r.seed)): "ok" if pd.notna(r.instances) else "timeout"
                for r in raw.itertuples()}
    rows = [(d, m, s, recorded.get((d, m, s)) or failure(log_dir, d, m, s))
            for d in datasets for m in methods for s in seeds]
    return pd.DataFrame(rows, columns=["dataset", "config", "seed", "status"])


def tuning_streams_table():
    """Accuracy and wall time of the selected configurations of both base learners on the tuning streams (seed 1)
    -> paper/tables/sgbt_tuning_streams.tex."""
    csv = os.path.join(EXP_DIR, "results", "raw", "sgbt_tuning.csv")
    summaries = {key: os.path.join(EXP_DIR, "results", "summary", f"sgbt_tuning_{key}.csv")
                 for key in ("sgbt_fimtdd", "sgbt_brim")}
    if not os.path.exists(csv) or not all(os.path.exists(f) for f in summaries.values()):
        print("[sgbt] tuning results not found, tuning table skipped")
        return
    raw = pd.read_csv(csv)
    datasets = read_list(os.path.join(EXP_DIR, "configs", "sgbt_tuning_datasets.txt"))
    best = {key: pd.read_csv(f, index_col=0).index[0] for key, f in summaries.items()}
    stopped = raw[raw["instances"].isna()]
    print(f"[sgbt] tuning: {len(raw)} runs, {len(stopped)} stopped at the time limit")
    for config, n in stopped.groupby("config").size().items():
        print(f"    {config}: {n} ({', '.join(stopped[stopped['config'] == config]['dataset'])})")
    rows = {key: raw[raw["config"] == config].groupby("dataset").mean(numeric_only=True).reindex(datasets)
            for key, config in best.items()}
    f, b = rows["sgbt_fimtdd"], rows["sgbt_brim"]
    lines = [r"\begin{tabular}{lrrrr}", r"\toprule",
             r"& \multicolumn{2}{c}{Accuracy (\%)} & \multicolumn{2}{c}{Wall time (s)} \\",
             r"\cmidrule(lr){2-3}\cmidrule(lr){4-5} Dataset & FIMT-DD & \brim & FIMT-DD & \brim \\", r"\midrule"]
    for d in datasets:
        fa, ba = f.loc[d, "accuracy"], b.loc[d, "accuracy"]
        cells = []
        for v, other in ((fa, ba), (b.loc[d, "accuracy"], fa)):
            cells.append("TO" if pd.isna(v) else rf"\textbf{{{v:.2f}}}" if pd.isna(other) or v >= other else f"{v:.2f}")
        cells += ["TO" if pd.isna(t) else f"{t:.0f}" for t in (f.loc[d, "learner_wall_s"], b.loc[d, "learner_wall_s"])]
        lines.append(tex(d) + " & " + " & ".join(cells) + r" \\")
    both = [d for d in datasets if pd.notna(f.loc[d, "accuracy"]) and pd.notna(b.loc[d, "accuracy"])]
    ratio = np.exp(np.log(b.loc[both, "learner_wall_s"] / f.loc[both, "learner_wall_s"]).mean())
    lines += [r"\midrule", f"Mean & {f.loc[both, 'accuracy'].mean():.2f} & {b.loc[both, 'accuracy'].mean():.2f} & "
              rf"\multicolumn{{2}}{{c}}{{\brim: {ratio:.1f}$\times$ (geom.\ mean)}} \\", r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", "sgbt_tuning_streams.tex"), "w") as out:
        out.write("\n".join(lines) + "\n")
    print(f"[sgbt] selected {best}: BRIM more accurate on "
          f"{(b.loc[both, 'accuracy'] > f.loc[both, 'accuracy']).sum()} of {len(both)} tuning streams where both "
          f"completed (mean {b.loc[both, 'accuracy'].mean():.2f} vs. {f.loc[both, 'accuracy'].mean():.2f}); "
          f"wall time ratio BRIM/FIMT-DD {ratio:.2f}")
    for d in datasets:
        print(f"    {d}: FIMT-DD {f.loc[d, 'accuracy']:.2f} ({f.loc[d, 'learner_wall_s']:.0f} s), "
              f"BRIM {b.loc[d, 'accuracy']:.2f} ({b.loc[d, 'learner_wall_s']:.0f} s)")


def final_stats(raw, status, datasets, methods):
    """Means and standard deviations over the completed seeds, and the numbers of completed, stopped and failed seeds."""
    done = raw[raw["instances"].notna()]
    agg = done.groupby(["dataset", "config"]).agg(
        **{f"{c}_{a}": (c, a) for c, *_ in FINAL_MEASURES for a in ("mean", "std")})
    counts = status.assign(
        completed=status["status"] == "ok", stopped=status["status"] == "timeout",
        failed=~status["status"].isin(["ok", "timeout"])).groupby(["dataset", "config"])[
        ["completed", "stopped", "failed"]].sum()
    reasons = status[status["status"] != "ok"].groupby(["dataset", "config"])["status"].agg(
        lambda s: "/".join(sorted(set(s))))
    index = pd.MultiIndex.from_product([datasets, methods], names=["dataset", "config"])
    stats = counts.reindex(index).join(agg).join(reasons.rename("not_completed"))
    stats.to_csv(os.path.join(EXP_DIR, "results", "summary", "sgbt_final.csv"))
    return stats


def final_ranks(stats, datasets, methods, alpha):
    """Ranks of the methods on each stream (no completed seed: last), average ranks, Friedman test and Shaffer's
    post-hoc test for each measure; CD diagrams."""
    summary_dir = os.path.join(EXP_DIR, "results", "summary")
    out, rows = {}, []
    for column, title, _, higher, short in FINAL_MEASURES:
        means = stats[f"{column}_mean"].unstack("config").reindex(index=datasets, columns=methods)
        ranks = rank_matrix(means.fillna(-np.inf if higher else np.inf).values, higher)
        avg = ranks.mean(axis=0)
        chi2, f, p = friedman(ranks)
        posthoc = shaffer_posthoc(avg, len(datasets), methods)
        posthoc.to_csv(os.path.join(summary_dir, f"sgbt_{short}_shaffer.csv"), index=False)
        pd.DataFrame(ranks, index=datasets, columns=methods).to_csv(os.path.join(summary_dir, f"sgbt_{short}_ranks.csv"))
        rows.append([re.sub(r" \(.*\)$", "", title)] + list(avg) + [p])
        print(f"[sgbt] {title}: N={len(datasets)} k={len(methods)} Friedman chi2={chi2:.2f} F={f:.2f} p={p:.3g}")
        for m, r in sorted(zip(methods, avg), key=lambda t: t[1]):
            print(f"    {m:18s} {r:.2f}")
        significant = posthoc[posthoc["p_shaffer"] <= alpha]
        for r in significant.itertuples():
            print(f"    significant: {r.method_a} ({r.rank_a:.2f}) vs. {r.method_b} ({r.rank_b:.2f}), "
                  f"p={r.p_shaffer:.3g}")
        # Robustness: the streams on which every method completed every seed
        complete = means.dropna().index[(stats["failed"] + stats["stopped"]).unstack("config")
                                        .reindex(index=means.dropna().index, columns=methods).sum(axis=1) == 0]
        if 2 <= len(complete) < len(datasets):
            sub = rank_matrix(means.loc[complete].values, higher).mean(axis=0)
            print(f"    on the {len(complete)} streams where all runs completed: "
                  + ", ".join(f"{m} {r:.2f}" for m, r in zip(methods, sub)))
        p_text = "p<1e-16" if p < 1e-16 else f"p={p:.2g}"
        names = [m.replace("SGBT-", "") for m in methods]
        cd_diagram(avg, names, posthoc.replace(dict(zip(methods, names))), alpha,
                   f"{title.replace(chr(92), '')} (Friedman {p_text})",
                   os.path.join(EXP_DIR, "paper", "figures", f"cd_sgbt_{short}"))
        out[short] = (means, ranks, avg, p)
    avg_df = pd.DataFrame(rows, columns=["measure"] + methods + ["friedman_p"])
    avg_df.to_csv(os.path.join(summary_dir, "sgbt_avg_ranks.csv"), index=False)
    lines = [r"\begin{tabular}{l" + "r" * len(methods) + "r}", r"\toprule",
             "Measure & " + " & ".join(label(m) for m in methods) + r" & Friedman $p$ \\", r"\midrule"]
    for r in avg_df.itertuples(index=False):
        vals = list(r[1:-1])
        cells = [rf"\textbf{{{v:.2f}}}" if v == min(vals) else f"{v:.2f}" for v in vals]
        p_cell = r"$<10^{-16}$" if r[-1] < 1e-16 else f"{r[-1]:.2g}"
        lines.append(r[0] + " & " + " & ".join(cells) + f" & {p_cell}" + r" \\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", "sgbt_avg_ranks.tex"), "w") as f:
        f.write("\n".join(lines) + "\n")
    return out


def final_table(stats, ranked, datasets, methods, num_seeds):
    """Per stream and method, mean +- sd over the completed seeds of each measure, one panel per measure with the
    average ranks -> paper/tables/sgbt_final.tex."""
    lines = [r"\begin{tabular}{l" + "r" * len(methods) + "}", r"\toprule",
             "Dataset & " + " & ".join(label(m) for m in methods) + r" \\"]
    for column, title, scale, higher, short in FINAL_MEASURES:
        means, _, avg, _ = ranked[short]
        lines += [r"\midrule", rf"\multicolumn{{{len(methods) + 1}}}{{l}}{{\emph{{{title}}}}} \\", r"\midrule"]
        for d in datasets:
            best = means.loc[d].max() if higher else means.loc[d].min()
            cells = []
            for m in methods:
                row = stats.loc[(d, m)]
                if row["completed"] == 0:
                    cells.append("/".join(SHORT_LABELS[s] for s in row["not_completed"].split("/")))
                    continue
                mean, sd = row[f"{column}_mean"] * scale, row[f"{column}_std"] * scale
                d_ = decimals(column, mean)
                cell = fmt(mean, d_) + (rf" $\pm$ {fmt(sd, d_)}" if row["completed"] > 1 else "")
                if fmt(mean, d_) == fmt(best * scale, d_):  # ties at the shown precision are all in bold
                    cell = rf"\textbf{{{cell}}}"
                if row["completed"] < num_seeds:
                    cell += rf"\textsuperscript{{{int(row['completed'])}}}"
                cells.append(cell)
            lines.append(tex(d) + " & " + " & ".join(cells) + r" \\")
        lines.append(r"\emph{Avg.\ rank} & " + " & ".join(
            rf"\textbf{{{v:.2f}}}" if v == min(avg) else f"{v:.2f}" for v in avg) + r" \\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", "sgbt_final.tex"), "w") as out:
        out.write("\n".join(lines) + "\n")
    # One table per measure, in the format of the per-dataset tables of analyze.py (mean with a small sd)
    captions = {"accuracy": r"Accuracy per dataset: mean $\pm$ standard deviation over three seeds (higher is better; "
                            r"best mean in bold; \%).",
                "memory": r"Final memory per dataset: mean $\pm$ standard deviation over three seeds (lower is better; "
                          r"best mean in bold; MB).",
                "time": r"Wall time per dataset: mean $\pm$ standard deviation over three seeds (lower is better; best "
                        r"mean in bold; seconds)."}
    for column, title, scale, higher, short in FINAL_MEASURES:
        means, _, avg, _ = ranked[short]
        lines = [r"\begin{table*}[t]", r"\centering", r"\small",
                 rf"\caption{{SGBT ensembles: {captions[short]}}}", rf"\label{{tab:sgbt-{short}}}",
                 r"\resizebox{\textwidth}{!}{\begin{tabular}{l" + "r" * len(methods) + "}", r"\toprule",
                 "Dataset & " + " & ".join(label(m) for m in methods) + r" \\", r"\midrule"]
        for d in datasets:
            best = means.loc[d].max() if higher else means.loc[d].min()
            cells = []
            for m in methods:
                row = stats.loc[(d, m)]
                if row["completed"] == 0:
                    cells.append("/".join(SHORT_LABELS[s] for s in row["not_completed"].split("/")))
                    continue
                mean, sd = row[f"{column}_mean"] * scale, row[f"{column}_std"] * scale
                d_ = decimals(column, mean)
                cell = fmt(mean, d_)
                if cell == fmt(best * scale, d_):
                    cell = rf"\textbf{{{cell}}}"
                if row["completed"] > 1:
                    cell += rf"\,{{\scriptsize$\pm${fmt(sd, decimals(column, sd))}}}"
                cells.append(cell)
            lines.append(tex(d) + " & " + " & ".join(cells) + r" \\")
        lines += [r"\midrule", "Avg. rank & " + " & ".join(f"{v:.2f}" for v in avg) + r" \\", r"\bottomrule",
                  r"\end{tabular}}", r"\end{table*}"]
        with open(os.path.join(EXP_DIR, "paper", "tables", f"sgbt_final_{short}.tex"), "w") as out:
            out.write("\n".join(lines) + "\n")


def final_comparisons(stats, datasets, methods):
    """Prints the numbers of the text: BRIM vs. FIMT-DD where both completed, and the budgeted variants vs. BRIM."""
    mean = {c: stats[f"{c}_mean"].unstack("config").reindex(datasets) for c, *_ in FINAL_MEASURES}
    acc, mem, time = mean["accuracy"], mean["mem_final_bytes"], mean["learner_wall_s"]

    def compare(a, b):
        both = acc[[a, b]].dropna().index
        diff = acc.loc[both, a] - acc.loc[both, b]
        print(f"[sgbt] {a} vs. {b} on the {len(both)} streams where both completed a seed: more accurate on "
              f"{(diff > 0).sum()}, mean difference {diff.mean():+.2f} points (median {diff.median():+.2f}); "
              f"geometric mean ratio of memory {np.exp(np.log(mem.loc[both, a] / mem.loc[both, b]).mean()):.3f}, "
              f"of wall time {np.exp(np.log(time.loc[both, a] / time.loc[both, b]).mean()):.2f}")
        for d in both:
            print(f"    {d:12s} {acc.loc[d, a]:6.2f} vs. {acc.loc[d, b]:6.2f} ({diff[d]:+6.2f}), memory "
                  f"{mem.loc[d, a] / 1e6:9.1f} vs. {mem.loc[d, b] / 1e6:9.1f} MB ({mem.loc[d, a] / mem.loc[d, b]:.3f}), "
                  f"time {time.loc[d, a]:7.0f} vs. {time.loc[d, b]:7.0f} s ({time.loc[d, a] / time.loc[d, b]:.2f})")

    fimtdd, brim = methods[0], methods[1]
    compare(brim, fimtdd)
    for m in methods[2:]:
        compare(m, brim)
        compare(m, fimtdd)
    print("[sgbt] geometric mean of the final memory (MB) and wall time (s) over the streams where a method completed:")
    for m in methods:
        ok = mem[m].dropna().index
        print(f"    {m:18s} {len(ok):2d} streams, memory {np.exp(np.log(mem.loc[ok, m]).mean()) / 1e6:9.1f} MB, "
              f"time {np.exp(np.log(time.loc[ok, m]).mean()):7.0f} s, max memory {mem.loc[ok, m].max() / 1e6:9.1f} MB "
              f"({mem.loc[ok, m].idxmax()})")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--alpha", type=float, default=0.05)
    args = parser.parse_args()
    for sub in ("results/summary", "paper/tables", "paper/figures"):
        os.makedirs(os.path.join(EXP_DIR, sub), exist_ok=True)
    tuning_streams_table()
    csv = os.path.join(EXP_DIR, "results", "raw", "sgbt.csv")
    if not os.path.exists(csv):
        print(f"[sgbt] {csv} not found (final runs not finished), skipped")
        return
    methods = read_list(os.path.join(EXP_DIR, "configs", "sgbt_methods.tsv"))
    datasets = read_list(os.path.join(EXP_DIR, "configs", "sgbt_datasets.txt"))
    raw = pd.read_csv(csv)
    seeds = sorted(int(s) for s in raw["seed"].unique())
    bad = raw[raw["nonfinite_votes"] > 0]
    if len(bad):
        print(f"[sgbt] runs with non-finite votes:\n{bad[['dataset', 'config', 'seed', 'nonfinite_votes']]}")
    status = run_status(raw, datasets, methods, seeds)
    status.to_csv(os.path.join(EXP_DIR, "results", "summary", "sgbt_final_status.csv"), index=False)
    print(f"[sgbt] final runs: {len(status)} expected, " + ", ".join(
        f"{n} {s}" for s, n in status["status"].value_counts().items()))
    for s in ("timeout", "oom", "killed", "failed", "missing"):
        sel = status[status["status"] == s]
        if len(sel):
            print(f"  {s} (method: dataset seeds):")
            for m, g in sel.groupby("config", sort=False):
                print(f"    {m}: " + "; ".join(f"{d} {','.join(map(str, gg['seed']))}"
                                               for d, gg in g.groupby("dataset", sort=False)))
    stats = final_stats(raw, status, datasets, methods)
    ranked = final_ranks(stats, datasets, methods, args.alpha)
    final_table(stats, ranked, datasets, methods, len(seeds))
    final_comparisons(stats, datasets, methods)


if __name__ == "__main__":
    main()
