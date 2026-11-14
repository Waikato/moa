#!/bin/bash
# One ensemble tuning job: all configurations of a grid on one dataset with one seed.
# Usage: ensemble_tune_job.sh <grid> <dataset> <seed> [<part> <parts>]   (grid: configs/ensemble_tuning/<grid>.tsv)
# With <part> and <parts>, only every <parts>-th configuration of the grid, starting at the <part>-th (0-based), so that
# a large grid (BRIM) runs as several jobs per dataset and seed.
source "$(dirname "$0")/env.sh"
JOB="$1_$2_s$3${4:+_p$4}"  # the files of each job are its own, since jobs run concurrently
LIST="$BUILD_DIR/tuning_lists/ensemble_$JOB.txt"; mkdir -p "$(dirname "$LIST")" "$EXP_DIR/results/raw/ensemble_tuning"
echo "$2" > "$LIST"
GRID="$EXP_DIR/configs/ensemble_tuning/$1.tsv"
if [ $# -ge 5 ]; then
  PART_GRID="$BUILD_DIR/tuning_lists/ensemble_$JOB.tsv"
  grep -v '^#' "$GRID" | awk -v part="$4" -v parts="$5" '(NR - 1) % parts == part' > "$PART_GRID"
  GRID="$PART_GRID"
fi
"$EXP_DIR/scripts/run.sh" "$GRID" "$LIST" "$3" "$EXP_DIR/results/raw/ensemble_tuning/$JOB.csv" > /dev/null
