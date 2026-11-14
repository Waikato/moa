#!/bin/bash
# Final SGBT classification experiment: the methods of configs/sgbt_methods.tsv (written by analysis/select_sgbt.py:
# tuned SGBT-FIMTDD, tuned SGBT-BRIM, SGBT-BRIM-1MB, -500KB and -100KB, the fast SGBT-BRIM-fastgood and -fast, and SGBT with
# its default base learner) on the streams of
# configs/sgbt_datasets.txt (all streams of CDatasets), seeds 1-3, memory measured at the end of each run. A run that
# takes longer than FINAL_TIME_LIMIT (default 10h) is stopped and recorded with empty measures.
# One job per (dataset, method, seed), PARALLEL at a time -> results/raw/sgbt/parts/*.csv (resumable), merged into
# results/raw/sgbt.csv; then run analysis/analyze_sgbt.py.
#
# Environment: PARALLEL (default 4), SMALL_MIN_FREE_GB (default 16), FINAL_TIME_LIMIT, SEEDS (default 1 2 3), SGBT_DATA_DIR (default CDatasets),
# SGBT_METHODS and SGBT_DATASETS (default configs/sgbt_methods.tsv and configs/sgbt_datasets.txt;
# e.g., to rerun failed runs: the other runs are skipped, and all parts are merged again).
export TIME_LIMIT="${FINAL_TIME_LIMIT:-10h}"
source "$(dirname "$0")/env.sh"
export DATA_DIR="${SGBT_DATA_DIR:-$EXP_DIR/CDatasets}" PARALLEL="${PARALLEL:-4}"
# The jobs of the methods that cannot fill the heap (the budgeted BRIM variants, when LARGE_METHODS is set) start with
# less free memory than the others (see run_sgbt_parallel.sh)
export SMALL_MIN_FREE_GB="${SMALL_MIN_FREE_GB:-16}"
METHODS="${SGBT_METHODS:-$EXP_DIR/configs/sgbt_methods.tsv}"
[ -f "$METHODS" ] || { echo "$METHODS not found: run analysis/select_sgbt.py first"; exit 1; }
"$EXP_DIR/scripts/run_sgbt_parallel.sh" "$METHODS" "${SGBT_DATASETS:-$EXP_DIR/configs/sgbt_datasets.txt}" "${SEEDS:-1 2 3}" \
  "$EXP_DIR/results/raw/sgbt"
echo "sgbt final done"
