#!/bin/bash
# Ablation study: BRIM with one component removed at a time, small datasets, seeds 1-3.
# As in the paper, the peak memory over 20 evenly spaced measurements (the other experiments measure at the end only)
export MEMORY_SAMPLES="${MEMORY_SAMPLES:-20}"
source "$(dirname "$0")/env.sh"
"$EXP_DIR/scripts/run.sh" "$EXP_DIR/configs/ablation_variants.tsv" "$EXP_DIR/configs/ablation_datasets.txt" "1 2 3" "$EXP_DIR/results/raw/ablation.csv"
