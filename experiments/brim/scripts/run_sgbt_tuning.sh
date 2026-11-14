#!/bin/bash
# Tuning of the base learner of SGBT (StreamingGradientBoostedTrees) for classification: the grids of
# configs/sgbt_tuning/ (the single-tree grids of FIMT-DD, 27 configurations, and BRIM, 2,187, inside SGBT; written by
# scripts/gen_sgbt_grids.py) on the streams of configs/sgbt_tuning_datasets.txt
# (CDatasets_tuning, 20K instances each), memory measured at the end of each run. One job per (dataset, configuration,
# seed), PARALLEL at a time, the most expensive streams first; the configurations of the two grids are interleaved so
# that the memory-hungry FIMT-DD jobs (their JVMs grow towards the whole heap on the multi-class streams) do not all run
# at once.
# Writes results/raw/sgbt_tuning/parts/*.csv (resumable) and results/raw/sgbt_tuning.csv; then run analysis/select_sgbt.py.
#
# A run that takes longer than TUNING_TIME_LIMIT (default 1h) is stopped and ranks last on its dataset.
# Environment: PARALLEL (default 8), TUNING_TIME_LIMIT, SEEDS (default 1), SGBT_TUNING_DATA_DIR (default
# CDatasets_tuning)
export TIME_LIMIT="${TUNING_TIME_LIMIT:-1h}"
source "$(dirname "$0")/env.sh"
export DATA_DIR="${SGBT_TUNING_DATA_DIR:-$EXP_DIR/CDatasets_tuning}" PARALLEL="${PARALLEL:-8}"
METHODS="$BUILD_DIR/sgbt_tuning_methods.tsv"; mkdir -p "$BUILD_DIR"
paste -d '\n' <(grep -v '^#' "$EXP_DIR/configs/sgbt_tuning/sgbt_brim.tsv") \
              <(grep -v '^#' "$EXP_DIR/configs/sgbt_tuning/sgbt_fimtdd.tsv") | grep -v '^$' > "$METHODS"
"$EXP_DIR/scripts/run_sgbt_parallel.sh" "$METHODS" "$EXP_DIR/configs/sgbt_tuning_datasets.txt" "${SEEDS:-1}" \
  "$EXP_DIR/results/raw/sgbt_tuning"
echo "sgbt tuning done"
