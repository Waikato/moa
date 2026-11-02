#!/bin/bash
# One tuning job: all configurations of a method's grid on one dataset, seeds 1-3. Usage: tune_job.sh <method> <dataset>
# [<part> <parts>]; with <part> and <parts>, only every <parts>-th configuration of the grid, starting at the <part>-th
# (0-based), so that a large grid (BRIM) runs as several jobs per dataset. A run is stopped after TUNING_TIME_LIMIT
# (default 10m, far above the seconds a run takes on the tuning streams); see TIME_LIMIT in env.sh.
export TIME_LIMIT="${TUNING_TIME_LIMIT:-10m}"
source "$(dirname "$0")/env.sh"
JOB="$1_$2${3:+_p$3}"  # the files of each job are its own, since jobs run concurrently
LIST="$BUILD_DIR/tuning_lists/$JOB.txt"; mkdir -p "$(dirname "$LIST")" "$EXP_DIR/results/raw/tuning"
echo "$2" > "$LIST"
GRID="$EXP_DIR/configs/tuning/$1.tsv"
if [ $# -ge 4 ]; then
  PART_GRID="$BUILD_DIR/tuning_lists/$JOB.tsv"
  grep -v '^#' "$GRID" | awk -v part="$3" -v parts="$4" '(NR - 1) % parts == part' > "$PART_GRID"
  GRID="$PART_GRID"
fi
OUT="$EXP_DIR/results/raw/tuning/$JOB.csv"
"$EXP_DIR/scripts/run.sh" "$GRID" "$LIST" "1 2 3" "$OUT" > /dev/null
