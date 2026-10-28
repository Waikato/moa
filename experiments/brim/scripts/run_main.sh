#!/bin/bash
# Main experiments: all datasets, baselines and BRIM with several memory budgets, seeds 1-3.
source "$(dirname "$0")/env.sh"
"$EXP_DIR/scripts/run.sh" "$EXP_DIR/configs/main_methods.tsv" "$EXP_DIR/configs/main_datasets.txt" "1 2 3" "$EXP_DIR/results/raw/main.csv"
