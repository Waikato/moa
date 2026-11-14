#!/bin/bash
# Runs the whole ensemble experiment unattended (e.g., in tmux on a server): tuning of the base learners, selection of
# their configurations, then the ensembles on all datasets. Every step is resumable, so the script can be restarted.
#
# Environment: TUNING_PARALLEL (default 16) and MAIN_PARALLEL (default 1, i.e., sequential) jobs, TUNING_TIME_LIMIT
# (default 20m) and FINAL_TIME_LIMIT (default 3h) per run, TUNING_HEAP (default 6g: 16 tuning runs with the 30 GB heap of
# the final runs exhaust the memory of a 125 GB server, although the largest tuning model, SGBR with FIMT-DD, retains at
# most about 2.2 GB), PYTHON (a Python 3 with
# pandas and scipy, default python3), and the variables of env.sh (DATA_DIR, SIZEOF_AGENT, ...).
set -uo pipefail
source "$(dirname "$0")/env.sh"
PYTHON="${PYTHON:-python3}"
echo "$(date '+%F %T') tuning (PARALLEL=${TUNING_PARALLEL:-16}, heap ${TUNING_HEAP:-6g})"
HEAP="${TUNING_HEAP:-6g}" PARALLEL="${TUNING_PARALLEL:-16}" "$EXP_DIR/scripts/run_ensemble_tuning.sh" || exit 1
echo "$(date '+%F %T') selection"
"$PYTHON" "$EXP_DIR/analysis/select_ensembles.py" || exit 1
cat "$EXP_DIR/configs/ensemble_methods.tsv"
echo "$(date '+%F %T') ensembles (PARALLEL=${MAIN_PARALLEL:-1})"
PARALLEL="${MAIN_PARALLEL:-1}" "$EXP_DIR/scripts/run_ensemble.sh" || exit 1
echo "$(date '+%F %T') done"
