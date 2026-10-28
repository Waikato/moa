#!/usr/bin/env python3
"""Analysis of the BRIM experiments: per-dataset results, ranks, Friedman test, Shaffer post-hoc test and
critical difference (CD) diagrams.

Usage: python3 analysis/analyze.py [--alpha 0.05]

Reads results/raw/main.csv, results/raw/ablation.csv and results/raw/ensemble.csv (written by scripts/run_*.sh) and
writes
  results/summary/*.csv          per-dataset means over seeds, ranks, average ranks, adjusted p-values
  paper/figures/cd_*.pdf|png     CD diagrams (main experiments) annotated with the Shaffer post-hoc test
  paper/tables/*.tex             LaTeX tables used by the paper

Evaluation measures (means over the seeds of each dataset):
  adjusted R^2 (adj_r2, higher is better), peak memory measured with the SizeOf agent (mem_peak_bytes, lower is
  better) and the wall time spent in the learner (learner_wall_s, lower is better).
"""
import argparse
import itertools
import math
import os
from functools import lru_cache

import numpy as np
import pandas as pd
from scipy import stats

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
METRICS = [  # column, label, higher is better, short name
    ("adj_r2", r"Adjusted $R^2$", True, "adjr2"),
    ("mem_peak_bytes", "Peak memory", False, "memory"),
    ("learner_wall_s", "Wall time", False, "time"),
]


# --------------------------------------------------------------------------------------------- statistics
def rank_matrix(values, higher_is_better):
    """Ranks the methods (columns) on each dataset (rows); rank 1 is the best, ties get average ranks."""
    data = -values if higher_is_better else values
    return np.vstack([stats.rankdata(row, method="average") for row in data])


def friedman(ranks):
    """Friedman test with the Iman-Davenport correction (Demsar, 2006). Returns (chi2, F, p-value of F)."""
    n, k = ranks.shape
    avg = ranks.mean(axis=0)
    chi2 = 12.0 * n / (k * (k + 1)) * (np.sum(avg ** 2) - k * (k + 1) ** 2 / 4.0)
    denominator = n * (k - 1) - chi2
    if denominator <= 0:
        return chi2, float("inf"), 0.0
    f = (n - 1) * chi2 / denominator
    p = 1.0 - stats.f.cdf(f, k - 1, (k - 1) * (n - 1))
    return chi2, f, p


@lru_cache(maxsize=None)
def shaffer_set(k):
    """S(k): the possible numbers of true hypotheses among the k(k-1)/2 pairwise comparisons of k methods
    (Shaffer, 1986), computed with the recursion S(k) = U_{j=1..k} { C(j,2) + x : x in S(k-j) }."""
    if k <= 1:
        return frozenset([0])
    result = set()
    for j in range(1, k + 1):
        for x in shaffer_set(k - j):
            result.add(math.comb(j, 2) + x)
    return frozenset(result)


def shaffer_posthoc(avg_ranks, n, names):
    """All pairwise comparisons of the average ranks (z test, Demsar 2006) with p-values adjusted by Shaffer's
    static procedure (Garcia and Herrera, 2008). Returns a DataFrame sorted by p-value."""
    k = len(names)
    se = math.sqrt(k * (k + 1) / (6.0 * n))
    rows = []
    for i, j in itertools.combinations(range(k), 2):
        z = (avg_ranks[i] - avg_ranks[j]) / se
        p = 2.0 * (1.0 - stats.norm.cdf(abs(z)))
        rows.append((names[i], names[j], avg_ranks[i], avg_ranks[j], z, p))
    df = pd.DataFrame(rows, columns=["method_a", "method_b", "rank_a", "rank_b", "z", "p"]).sort_values("p")
    m = len(df)
    s = sorted(shaffer_set(k))
    adjusted, running = [], 0.0
    for i, p in enumerate(df["p"].values, start=1):
        t = max(x for x in s if x <= m - i + 1)  # maximum number of true hypotheses given i-1 rejections
        running = max(running, min(1.0, t * p))
        adjusted.append(running)
    df["p_shaffer"] = adjusted
    return df.reset_index(drop=True)


def nonsignificant_groups(names_sorted, posthoc, alpha):
    """Maximal groups of methods, consecutive in rank order, without significant differences between any two."""
    significant = set()
    for _, r in posthoc.iterrows():
        if r["p_shaffer"] <= alpha:
            significant.add(frozenset((r["method_a"], r["method_b"])))
    groups = []
    k = len(names_sorted)
    for start in range(k):
        end = start
        while end + 1 < k and all(frozenset((names_sorted[a], names_sorted[end + 1])) not in significant
                                  for a in range(start, end + 1)):
            end += 1
        if end > start and not any(s <= start and end <= e for s, e in groups):
            groups.append((start, end))
    return groups


# --------------------------------------------------------------------------------------------- CD diagram
def cd_diagram(avg_ranks, names, posthoc, alpha, title, path):
    """Critical difference diagram in the style of Demsar (2006). The bars link methods that are not
    significantly different according to the Shaffer post-hoc test."""
    order = np.argsort(avg_ranks)
    names_sorted = [names[i] for i in order]
    ranks_sorted = [avg_ranks[i] for i in order]
    k = len(names)
    groups = nonsignificant_groups(names_sorted, posthoc, alpha)

    width, height = 6.0, 1.2 + 0.28 * ((k + 1) // 2) + 0.12 * len(groups)
    fig, ax = plt.subplots(figsize=(width, height))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    left, right, axis_y = 0.18, 0.82, 0.82

    def x_of(rank):  # rank 1 on the left
        return left + (rank - 1) / (k - 1) * (right - left)

    ax.plot([left, right], [axis_y, axis_y], color="black", lw=1)
    for r in range(1, k + 1):
        ax.plot([x_of(r)] * 2, [axis_y, axis_y + 0.03], color="black", lw=1)
        ax.text(x_of(r), axis_y + 0.05, str(r), ha="center", va="bottom", fontsize=8)
    ax.set_title(title, fontsize=9, pad=14)

    step = (axis_y - 0.12) / max(1, (k + 1) // 2 + 1)
    half = (k + 1) // 2
    for idx, (name, rank) in enumerate(zip(names_sorted, ranks_sorted)):
        if idx < half:  # best methods: labels on the left
            y = axis_y - step * (idx + 1)
            ax.plot([x_of(rank), x_of(rank), left - 0.02], [axis_y, y, y], color="black", lw=0.8)
            ax.text(left - 0.03, y, f"{name} ({rank:.2f})", ha="right", va="center", fontsize=8)
        else:
            y = axis_y - step * (k - idx)
            ax.plot([x_of(rank), x_of(rank), right + 0.02], [axis_y, y, y], color="black", lw=0.8)
            ax.text(right + 0.03, y, f"({rank:.2f}) {name}", ha="left", va="center", fontsize=8)

    for g, (s, e) in enumerate(groups):
        y = axis_y - 0.04 - 0.035 * g
        ax.plot([x_of(ranks_sorted[s]) - 0.005, x_of(ranks_sorted[e]) + 0.005], [y, y], color="black", lw=2.5)
    ax.text(0.5, 0.01, f"Bars: no significant difference (Friedman + Shaffer post-hoc, $\\alpha$={alpha})",
            ha="center", va="bottom", fontsize=7)
    fig.savefig(path + ".pdf", bbox_inches="tight")
    fig.savefig(path + ".png", bbox_inches="tight", dpi=200)
    plt.close(fig)


# --------------------------------------------------------------------------------------------- tables
def fmt_value(column, v, unit=None):
    """Formats a mean or a standard deviation. For memory, unit is "K" or "M" (the unit of the mean)."""
    if column == "adj_r2":
        return f"{v:.3f}" if abs(v) < 10 else (r"$<\!-10$" if v < 0 else r"$>\!10$")
    if column == "mem_peak_bytes":
        unit = unit or ("K" if v < 1e6 else "M")
        return f"{v / 1e3:.0f}" if unit == "K" else f"{v / 1e6:.1f}M"
    return f"{v:.2f}" if v < 100 else f"{v:.0f}"


def fmt_cell(column, mean, sd):
    """Mean and standard deviation over the seeds. The deviation is omitted when the mean is not shown (diverging
    adjusted R^2), and shown in the unit of the mean for memory."""
    cell = fmt_value(column, mean)
    if column == "adj_r2" and abs(mean) >= 10:
        return cell
    unit = None if column != "mem_peak_bytes" else ("K" if mean < 1e6 else "M")
    sd_text = fmt_value(column, sd, unit) if not (column == "adj_r2" and abs(sd) >= 10) else r"$>\!10$"
    return cell + r"\,{\scriptsize$\pm$" + sd_text.replace("M", "") + "}"


def latex_table(means, sds, ranks, methods, column, label, caption, path, best_high):
    wide = len(methods) > 7  # rotated headers and scaled to the text width
    header = [rf"\rotatebox{{90}}{{{m}}}" if wide else m for m in methods]
    lines = [r"\begin{table*}[t]", r"\centering", r"\small", rf"\caption{{{caption}}}", rf"\label{{{label}}}",
             (r"\resizebox{\textwidth}{!}{" if wide else "") + r"\begin{tabular}{l" + "r" * len(methods) + "}",
             r"\toprule", "Dataset & " + " & ".join(header) + r" \\", r"\midrule"]
    for dataset, row in means.iterrows():
        values = row[methods].values.astype(float)
        deviations = sds.loc[dataset, methods].values.astype(float)
        best = np.nanmax(values) if best_high else np.nanmin(values)
        cells = []
        for v, sd in zip(values, deviations):
            cell = fmt_cell(column, v, 0.0 if np.isnan(sd) else sd)
            if v == best:  # bold mean, normal deviation
                parts = cell.split(r"\,", 1)
                cell = rf"\textbf{{{parts[0]}}}" + (r"\," + parts[1] if len(parts) > 1 else "")
            cells.append(cell)
        lines.append(dataset.replace("_", r"\_") + " & " + " & ".join(cells) + r" \\")
    lines += [r"\midrule", "Avg. rank & " + " & ".join(f"{r:.2f}" for r in ranks.mean(axis=0)) + r" \\",
              r"\bottomrule", r"\end{tabular}" + ("}" if wide else ""), r"\end{table*}"]
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


def analyze(name, csv, alpha, cd, method_order):
    if not os.path.exists(csv):
        print(f"[{name}] {csv} not found, skipped")
        return None
    raw = pd.read_csv(csv)
    summary_dir = os.path.join(EXP_DIR, "results", "summary")
    os.makedirs(summary_dir, exist_ok=True)
    means = raw.groupby(["dataset", "config"]).mean(numeric_only=True).reset_index()
    means.to_csv(os.path.join(summary_dir, f"{name}_means.csv"), index=False)
    sds = raw.groupby(["dataset", "config"]).std(numeric_only=True, ddof=1).reset_index()
    sds.to_csv(os.path.join(summary_dir, f"{name}_stds.csv"), index=False)
    methods = [m for m in method_order if m in set(means["config"])]
    out = {}
    avg_rank_rows = []
    for column, label, higher, short in METRICS:
        table = means.pivot(index="dataset", columns="config", values=column)[methods].dropna()
        ranks = rank_matrix(table.values, higher)
        avg = ranks.mean(axis=0)
        chi2, f, p = friedman(ranks)
        posthoc = shaffer_posthoc(avg, len(table), methods)
        posthoc.to_csv(os.path.join(summary_dir, f"{name}_{short}_shaffer.csv"), index=False)
        pd.DataFrame(ranks, index=table.index, columns=methods).to_csv(
            os.path.join(summary_dir, f"{name}_{short}_ranks.csv"))
        avg_rank_rows.append([label] + list(avg) + [p])
        print(f"[{name}] {label}: N={len(table)} k={len(methods)} Friedman chi2={chi2:.2f} F={f:.2f} p={p:.3g}")
        for m, r in sorted(zip(methods, avg), key=lambda t: t[1]):
            print(f"    {m:24s} {r:.2f}")
        if cd:
            p_text = "p<1e-16" if p < 1e-16 else f"p={p:.2g}"
            cd_diagram(avg, methods, posthoc, alpha, f"{label} (Friedman {p_text})",
                       os.path.join(EXP_DIR, "paper", "figures", f"cd_{name}_{short}"))
        sd_table = sds.pivot(index="dataset", columns="config", values=column).reindex(index=table.index, columns=methods)
        latex_table(table, sd_table, ranks, methods, column, f"tab:{name}-{short}",
                    f"{label} per dataset: mean $\\pm$ standard deviation over three seeds "
                    f"({'higher' if higher else 'lower'} is better; best mean in bold"
                    f"{'; KB unless marked M for MB' if short == 'memory' else '; seconds' if short == 'time' else ''}).",
                    os.path.join(EXP_DIR, "paper", "tables", f"{name}_{short}.tex"), higher)
        out[short] = (table, ranks, avg, p, posthoc)
    avg_df = pd.DataFrame(avg_rank_rows, columns=["measure"] + methods + ["friedman_p"])
    avg_df.to_csv(os.path.join(summary_dir, f"{name}_avg_ranks.csv"), index=False)
    # LaTeX table of the average ranks
    header = [rf"\rotatebox{{90}}{{{m}}}" if len(methods) > 7 else m for m in methods]
    lines = [r"\begin{tabular}{l" + "r" * len(methods) + "r}", r"\toprule",
             "Measure & " + " & ".join(header) + r" & Friedman $p$ \\", r"\midrule"]
    for _, r in avg_df.iterrows():
        vals = [r[m] for m in methods]
        best = min(vals)
        cells = [rf"\textbf{{{v:.2f}}}" if v == best else f"{v:.2f}" for v in vals]
        p_cell = r"$<10^{-16}$" if r["friedman_p"] < 1e-16 else f"{r['friedman_p']:.2g}"
        lines.append(r["measure"] + " & " + " & ".join(cells) + f" & {p_cell}" + r" \\")
    lines += [r"\bottomrule", r"\end{tabular}"]
    with open(os.path.join(EXP_DIR, "paper", "tables", f"{name}_avg_ranks.tex"), "w") as f:
        f.write("\n".join(lines) + "\n")
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--alpha", type=float, default=0.05)
    args = parser.parse_args()
    os.makedirs(os.path.join(EXP_DIR, "paper", "figures"), exist_ok=True)
    os.makedirs(os.path.join(EXP_DIR, "paper", "tables"), exist_ok=True)
    main_methods = [l.split("\t")[0] for l in open(os.path.join(EXP_DIR, "configs", "main_methods.tsv"))
                    if l.strip() and not l.startswith("#")]
    ablation_methods = [l.split("\t")[0] for l in open(os.path.join(EXP_DIR, "configs", "ablation_variants.tsv"))
                        if l.strip() and not l.startswith("#")]
    analyze("main", os.path.join(EXP_DIR, "results", "raw", "main.csv"), args.alpha, True, main_methods)
    analyze("ablation", os.path.join(EXP_DIR, "results", "raw", "ablation.csv"), args.alpha, False, ablation_methods)
    ensemble_config = os.path.join(EXP_DIR, "configs", "ensemble_methods.tsv")
    if os.path.exists(ensemble_config):
        ensemble_methods = [l.split("\t")[0] for l in open(ensemble_config) if l.strip() and not l.startswith("#")]
        analyze("ensemble", os.path.join(EXP_DIR, "results", "raw", "ensemble.csv"), args.alpha, True, ensemble_methods)


if __name__ == "__main__":
    main()
