#!/bin/bash
# Runs the single-tree experiments unattended (e.g., in tmux on a server): tuning of the baselines, selection of their
# configurations, then the main experiments on all datasets. Memory is measured once, at the end of each stream
# (MEMORY_SAMPLES=1), in both the tuning and the main runs. Every step is resumable, so the script can be restarted.
#
# Environment: TUNING_PARALLEL (default 4) jobs and TUNING_HEAP (default 4g) per job for tuning; the main runs are
# sequential (one JVM at a time, so that wall times are comparable) with HEAP (default 8g). PYTHON (a Python 3 with
# pandas and scipy, default python3), and the variables of env.sh (DATA_DIR, SIZEOF_AGENT, ...).
set -uo pipefail
source "$(dirname "$0")/env.sh"
PYTHON="${PYTHON:-python3}"
echo "$(date '+%F %T') tuning (PARALLEL=${TUNING_PARALLEL:-4}, HEAP=${TUNING_HEAP:-4g})"
PARALLEL="${TUNING_PARALLEL:-4}" HEAP="${TUNING_HEAP:-4g}" "$EXP_DIR/scripts/run_tuning.sh" || exit 1
echo "$(date '+%F %T') selection"
mkdir -p "$EXP_DIR/paper/tables"
"$PYTHON" "$EXP_DIR/analysis/select_baselines.py" || exit 1
cat "$EXP_DIR/configs/main_methods.tsv"
echo "$(date '+%F %T') main (sequential, MEMORY_SAMPLES=1)"
MEMORY_SAMPLES=1 "$EXP_DIR/scripts/run_main.sh" || exit 1
echo "$(date '+%F %T') done"
