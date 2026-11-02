#!/bin/bash
# Hyperparameter tuning of BRIM and the baselines on the 10 streams of configs/tuning_datasets.txt (the ablation streams
# without hyperA), seeds 1-3. Runs PARALLEL jobs at a time, since tuning selects by accuracy and timing contention does
# not matter: one job per method
# and dataset, and BRIM_PARTS jobs per dataset for the BRIM grid (2187 configurations). The BRIM jobs come first, since
# they are the longest.
# Writes results/raw/tuning/<method>_<dataset>[_p<part>].csv (resumable); then run analysis/select_baselines.py.
source "$(dirname "$0")/env.sh"
PARALLEL="${PARALLEL:-8}"
BRIM_PARTS="${BRIM_PARTS:-12}"
{
  for part in $(seq 0 $((BRIM_PARTS - 1))); do
    grep -v '^#' "$EXP_DIR/configs/tuning_datasets.txt" | sed "s/^/brim /; s/$/ $part $BRIM_PARTS/"
  done
  for method in fimtdd orto amrules sgt; do
    grep -v '^#' "$EXP_DIR/configs/tuning_datasets.txt" | sed "s/^/$method /"
  done
} | xargs -P "$PARALLEL" -L 1 "$EXP_DIR/scripts/tune_job.sh"
echo "tuning done"
