#!/bin/bash
# Tuning of the base learners inside OzaBag and SGBR on the 10 streams of configs/ensemble_tuning_datasets.txt (the
# ablation streams without hyperA, the most expensive one), seeds 1-3.
# Runs PARALLEL jobs at a time: one per grid, dataset and seed, and BRIM_PARTS jobs per dataset and seed for the BRIM grids
# (2,187 configurations). The FIMT-DD grids and OzaBag-BRIM come first, then SGBR-BRIM, the longest; within a grid, the
# largest datasets come first, so that the longest jobs do not start last.
# Writes results/raw/ensemble_tuning/<grid>_<dataset>_s<seed>[_p<part>].csv (resumable); then run
# analysis/select_ensembles.py.
#
# Memory is not measured (the selection uses the adjusted R^2 only), and a run that takes longer than TUNING_TIME_LIMIT
# (default 20 minutes) is stopped and ranks last on its dataset.
# Environment: PARALLEL (default 4), TUNING_TIME_LIMIT, and GRIDS / SEEDS to run a subset, e.g., to split the tuning
# across machines: GRIDS="sgbr_brim" SEEDS="1 2" scripts/run_ensemble_tuning.sh
export TIME_LIMIT="${TUNING_TIME_LIMIT:-20m}"
export MEMORY_SAMPLES="${MEMORY_SAMPLES:-0}"
source "$(dirname "$0")/env.sh"
PARALLEL="${PARALLEL:-4}"
GRIDS="${GRIDS:-oza_fimtdd sgbr_fimtdd oza_brim sgbr_brim}"
SEEDS="${SEEDS:-1 2 3}"
BRIM_PARTS="${BRIM_PARTS:-27}"
# Datasets by decreasing ARFF size
DATASETS=$(for dataset in $(grep -v '^#' "$EXP_DIR/configs/ensemble_tuning_datasets.txt"); do
  echo "$(wc -c < "$DATA_DIR/$dataset.arff") $dataset"
done | sort -rn | cut -d' ' -f2)
for grid in $GRIDS; do
  for dataset in $DATASETS; do
    for seed in $SEEDS; do
      if [[ "$grid" == *_brim ]]; then
        for part in $(seq 0 $((BRIM_PARTS - 1))); do echo "$grid $dataset $seed $part $BRIM_PARTS"; done
      else
        echo "$grid $dataset $seed"
      fi
    done
  done
done | xargs -P "$PARALLEL" -L 1 "$EXP_DIR/scripts/ensemble_tune_job.sh"
echo "ensemble tuning done"
