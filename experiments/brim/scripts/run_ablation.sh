#!/bin/bash
# Ablation study: BRIM with one component removed at a time, small datasets, seeds 1-3.
source "$(dirname "$0")/env.sh"
"$EXP_DIR/scripts/run.sh" "$EXP_DIR/configs/ablation_variants.tsv" "$EXP_DIR/configs/ablation_datasets.txt" "1 2 3" "$EXP_DIR/results/raw/ablation.csv"
