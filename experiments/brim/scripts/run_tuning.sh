#!/bin/bash
# Hyperparameter tuning of the baselines on the 11 small streams of the ablation study (seeds 1-3). Runs PARALLEL jobs
# (one per method and dataset) at a time, since tuning selects by accuracy and timing contention does not matter.
# Writes results/raw/tuning/<method>_<dataset>.csv (resumable); then run analysis/select_baselines.py.
source "$(dirname "$0")/env.sh"
PARALLEL="${PARALLEL:-8}"
for method in fimtdd orto amrules sgt; do
  grep -v '^#' "$EXP_DIR/configs/ablation_datasets.txt" | sed "s/^/$method /"
done | xargs -P "$PARALLEL" -L 1 "$EXP_DIR/scripts/tune_job.sh"
echo "tuning done"
