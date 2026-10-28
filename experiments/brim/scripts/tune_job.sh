#!/bin/bash
# One tuning job: all configurations of a method's grid on one dataset, seeds 1-3. Usage: tune_job.sh <method> <dataset>
source "$(dirname "$0")/env.sh"
export MEMORY_SAMPLES=1
LIST="$BUILD_DIR/tuning_lists/$2.txt"; mkdir -p "$(dirname "$LIST")" "$EXP_DIR/results/raw/tuning"
echo "$2" > "$LIST"
"$EXP_DIR/scripts/run.sh" "$EXP_DIR/configs/tuning/$1.tsv" "$LIST" "1 2 3" "$EXP_DIR/results/raw/tuning/$1_$2.csv" > /dev/null
