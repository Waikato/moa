#!/bin/bash
# Ensemble experiments: OzaBag and SGBR with FIMT-DD and BRIM base learners (configurations selected by
# analysis/select_ensembles.py), all datasets, seeds 1-3 -> results/raw/ensemble.csv.
#
# By default the runs are sequential, as in the main experiments, so that wall times are comparable. With PARALLEL > 1,
# PARALLEL jobs (one per dataset and seed, largest datasets first) run at a time and write
# results/raw/ensemble_parts/<dataset>_s<seed>.csv, which are merged into results/raw/ensemble.csv at the end; wall times
# are then affected by the concurrent jobs. Both modes are resumable.
#
# Memory is measured at the end of each run, and a run that takes longer than FINAL_TIME_LIMIT (default 3h) is stopped
# and recorded with empty measures.
# Environment: PARALLEL (default 1), FINAL_TIME_LIMIT, and METHODS / DATASETS / OUT to override the input and output
# files.
export TIME_LIMIT="${FINAL_TIME_LIMIT:-3h}"
source "$(dirname "$0")/env.sh"
PARALLEL="${PARALLEL:-1}"
METHODS="${METHODS:-$EXP_DIR/configs/ensemble_methods.tsv}"
DATASETS="${DATASETS:-$EXP_DIR/configs/main_datasets.txt}"
OUT="${OUT:-$EXP_DIR/results/raw/ensemble.csv}"
SEEDS="${SEEDS:-1 2 3}"
[ -f "$METHODS" ] || { echo "$METHODS not found: run analysis/select_ensembles.py first"; exit 1; }

if [ "$PARALLEL" -le 1 ]; then
  "$EXP_DIR/scripts/run.sh" "$METHODS" "$DATASETS" "$SEEDS" "$OUT"
  exit
fi

PARTS="$(dirname "$OUT")/ensemble_parts"; mkdir -p "$PARTS" "$BUILD_DIR/tuning_lists"
export EXP_DIR METHODS PARTS BUILD_DIR
# Largest ARFF files first, so that the longest jobs do not start last
for dataset in $(grep -v '^#' "$DATASETS"); do
  echo "$(wc -c < "$DATA_DIR/$dataset.arff") $dataset"
done | sort -rn | while read -r _ dataset; do
  for seed in $SEEDS; do echo "$dataset $seed"; done
done | xargs -P "$PARALLEL" -L 1 bash -c '
  echo "$0" > "$BUILD_DIR/tuning_lists/ensemble_$0.txt"
  "$EXP_DIR/scripts/run.sh" "$METHODS" "$BUILD_DIR/tuning_lists/ensemble_$0.txt" "$1" "$PARTS/$0_s$1.csv"'

# Merge the parts (one header)
first=1
for part in "$PARTS"/*.csv; do
  if [ $first -eq 1 ]; then cat "$part"; first=0; else tail -n +2 "$part"; fi
done > "$OUT"
echo "merged $(($(wc -l < "$OUT") - 1)) runs into $OUT"
