#!/bin/bash
# Runs the single-tree experiments unattended (e.g., in tmux on a server): tuning of BRIM and the baselines, selection
# of their configurations, the main experiments on all datasets, then the ablation of the selected BRIM. Memory is
# measured once, at the end of each stream, in the tuning and the main runs (the ablation keeps its peak over 20
# measurements). Every step is resumable, so the script can be restarted.
#
# Environment: TUNING_PARALLEL (default 4) jobs for tuning and BRIM_PARTS (run_tuning.sh); the main and ablation runs
# are sequential (one JVM at a time, so that wall times are comparable). PYTHON (a Python 3 with pandas and scipy,
# default python3), and the variables of env.sh (DATA_DIR, SIZEOF_AGENT, ...).
set -uo pipefail
source "$(dirname "$0")/env.sh"
PYTHON="${PYTHON:-python3}"
echo "$(date '+%F %T') tuning (PARALLEL=${TUNING_PARALLEL:-4})"
PARALLEL="${TUNING_PARALLEL:-4}" "$EXP_DIR/scripts/run_tuning.sh" || exit 1
echo "$(date '+%F %T') selection"
mkdir -p "$EXP_DIR/paper/tables"
"$PYTHON" "$EXP_DIR/analysis/select_baselines.py" || exit 1
cat "$EXP_DIR/configs/main_methods.tsv" "$EXP_DIR/configs/ablation_variants.tsv"
echo "$(date '+%F %T') main (sequential)"
"$EXP_DIR/scripts/run_main.sh" || exit 1
echo "$(date '+%F %T') ablation (sequential)"
"$EXP_DIR/scripts/run_ablation.sh" || exit 1
echo "$(date '+%F %T') done"
