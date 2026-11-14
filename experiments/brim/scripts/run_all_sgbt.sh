#!/bin/bash
# Runs the whole SGBT classification experiment unattended (e.g., in tmux on a server): tuning of the base learners,
# selection of their configurations, then the final runs. Every step is resumable, so the script can be restarted.
# The selection is not made while tuning runs are missing (failed, e.g., killed for lack of memory; a run stopped at
# the time limit is recorded): rerun the script to retry them, or set ALLOW_MISSING=1 to rank them last.
#
# The tuning (15,498 short runs of the full single-tree grids inside SGBT on 20K-instance streams) and the final runs
# (336 long runs on up to 1M instances) get their own heap and memory checks of run_sgbt_parallel.sh:
#
#   variable (TUNING_*, FINAL_*)  tuning  final  meaning
#   *_PARALLEL                    16      8      jobs at a time
#   *_HEAP                        8g      30g    maximum heap of each job
#   *_MIN_FREE_GB                 10      32     GB of memory available to start a job (room for one more heap)
#   *_START_GAP                   15      240    seconds between two starts
#   *_LARGE_PARALLEL              3       3      jobs at a time of the methods that can fill the heap (*_LARGE_METHODS:
#                                                FIMT-DD in the tuning; FIMT-DD, also with SGBT's default
#                                                configuration, and BRIM without a smaller budget, also the fast
#                                                configurations, in the final runs)
#
# Other variables: TUNING_TIME_LIMIT (default 1h), FINAL_TIME_LIMIT (default 10h), EMERGENCY_FREE_GB (default 6; see
# run_sgbt_parallel.sh), PYTHON (a Python 3 with pandas and scipy, default python3), and the variables of env.sh
# (SIZEOF_AGENT, ...).
set -uo pipefail
source "$(dirname "$0")/env.sh"
PYTHON="${PYTHON:-python3}"
export EMERGENCY_FREE_GB="${EMERGENCY_FREE_GB:-6}"
echo "$(date '+%F %T') tuning (PARALLEL=${TUNING_PARALLEL:-16}, HEAP=${TUNING_HEAP:-8g})"
PARALLEL="${TUNING_PARALLEL:-16}" HEAP="${TUNING_HEAP:-8g}" MIN_FREE_GB="${TUNING_MIN_FREE_GB:-10}" \
  START_GAP="${TUNING_START_GAP:-15}" LARGE_METHODS="${TUNING_LARGE_METHODS:-^SGBT-FIMTDD}" \
  LARGE_PARALLEL="${TUNING_LARGE_PARALLEL:-3}" "$EXP_DIR/scripts/run_sgbt_tuning.sh" || exit 1
expected=$(( $(cat "$EXP_DIR"/configs/sgbt_tuning/*.tsv | grep -v '^#' | grep -vc '^$') \
  * $(grep -v '^#' "$EXP_DIR/configs/sgbt_tuning_datasets.txt" | grep -vc '^$') * $(echo ${SEEDS:-1} | wc -w) ))
recorded=$(( $(wc -l < "$EXP_DIR/results/raw/sgbt_tuning.csv") - 1 ))
if [ "$recorded" -lt "$expected" ] && [ "${ALLOW_MISSING:-0}" != 1 ]; then
  echo "$(date '+%F %T') $recorded of $expected tuning runs recorded: rerun to retry the failed runs (results/logs/*.err)"
  exit 2
fi
echo "$(date '+%F %T') selection"
"$PYTHON" "$EXP_DIR/analysis/select_sgbt.py" || exit 1
cat "$EXP_DIR/configs/sgbt_methods.tsv"
echo "$(date '+%F %T') final runs (PARALLEL=${FINAL_PARALLEL:-8}, HEAP=${FINAL_HEAP:-30g})"
PARALLEL="${FINAL_PARALLEL:-8}" HEAP="${FINAL_HEAP:-30g}" MIN_FREE_GB="${FINAL_MIN_FREE_GB:-32}" \
  START_GAP="${FINAL_START_GAP:-240}" LARGE_METHODS="${FINAL_LARGE_METHODS:-^SGBT-(FIMTDD|BRIM|BRIM-fastgood|BRIM-fast|default)$}" \
  LARGE_PARALLEL="${FINAL_LARGE_PARALLEL:-3}" "$EXP_DIR/scripts/run_sgbt_final.sh" || exit 1
echo "$(date '+%F %T') done"
