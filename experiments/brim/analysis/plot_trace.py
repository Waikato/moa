#!/usr/bin/env python3
"""Plots the motivating experiment (results/raw/trace.csv, written by scripts/run_trace.sh): memory and cumulative wall
time along the stream. Writes paper/figures/trace.pdf and .png."""
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
trace = pd.read_csv(os.path.join(EXP_DIR, "results", "raw", "trace.csv"))
styles = {  # config prefix -> (label, line style)
    "Initial attempt (": ("Initial attempt: PLASTIC + variance reduction + E-BST", "-"),
    "Initial attempt +": ("Initial attempt + quantization observers", "--"),
    "BRIM": ("BRIM", "-."),
    "FIMTDD": ("FIMT-DD", ":"),
}

fig, axes = plt.subplots(1, 2, figsize=(7.2, 2.6))
for config, group in trace.groupby("config", sort=False):
    label, style = next(v for k, v in styles.items() if config.startswith(k))
    x = group["instances"] / 1000
    axes[0].plot(x, group["mem_bytes"] / 1e6, style, label=label, lw=1.4)
    axes[1].plot(x, group["learner_wall_s"], style, label=label, lw=1.4)
axes[0].set_yscale("log")
axes[0].set_ylabel("Model size (MB, log scale)")
axes[1].set_ylabel("Cumulative wall time (s)")
for ax in axes:
    ax.set_xlabel("Instances (thousands)")
    ax.grid(alpha=0.3, lw=0.5)
handles, labels = axes[0].get_legend_handles_labels()
fig.legend(handles, labels, loc="upper center", ncol=2, fontsize=7.5, frameon=False, bbox_to_anchor=(0.5, 1.08))
fig.tight_layout()
out = os.path.join(EXP_DIR, "paper", "figures", "trace")
fig.savefig(out + ".pdf", bbox_inches="tight")
fig.savefig(out + ".png", bbox_inches="tight", dpi=200)
print("wrote", out + ".pdf")
