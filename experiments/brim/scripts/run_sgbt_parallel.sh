#!/bin/bash
# Runs the SGBT classification experiment in parallel: one job per (dataset, method, seed), PARALLEL jobs at a time,
# the most expensive datasets (size x classes) first so that the longest jobs do not start last. Each job writes
# <out dir>/parts/<dataset>__<method>__s<seed>.csv with run_sgbt.sh (resumable); the parts are merged into <out dir>.csv.
# Each run uses one core (see FJ_PARALLELISM in run_sgbt.sh), so wall times are only mildly affected by the other jobs
# as long as PARALLEL stays below the number of free cores.
#
# Usage: scripts/run_sgbt_parallel.sh <methods.tsv> <datasets.txt> "<seeds>" <out dir>
# Environment: PARALLEL (default 8), LARGE_METHODS, LARGE_PARALLEL, MIN_FREE_GB, START_GAP, EMERGENCY_FREE_GB,
# RECOVER_MIN (see the scheduler below), DATA_DIR, HEAP per job, MEMORY_SAMPLES and the other variables of env.sh.
set -uo pipefail
source "$(dirname "$0")/env.sh"
METHODS="$1"; DATASETS="$2"; SEEDS="$3"; OUT_DIR="$4"
PARALLEL="${PARALLEL:-8}"
PARTS="$OUT_DIR/parts"; LISTS="$OUT_DIR/lists"; mkdir -p "$PARTS" "$LISTS"
export EXP_DIR PARTS LISTS

# One single-method file per method and one single-dataset list per dataset
while IFS=$'\t' read -r name cli; do
  [ -z "$name" ] && continue
  printf '%s\t%s\n' "$name" "$cli" > "$LISTS/method_$name.tsv"
done < <(grep -v '^#' "$METHODS")
# Expected cost: file size times the number of classes (SGBT trains one booster per class with more than two classes)
for dataset in $(grep -v '^#' "$DATASETS"); do
  echo "$dataset" > "$LISTS/dataset_$dataset.txt"
  classes=$(( $(grep -i '^@attribute' "$DATA_DIR/$dataset.arff" | tail -1 | tr -cd ',' | wc -c) + 1 ))
  echo "$(( $(wc -c < "$DATA_DIR/$dataset.arff") * (classes > 2 ? classes : 1) )) $dataset"
done | sort -rn | while read -r _ dataset; do
  for seed in $SEEDS; do
    grep -v '^#' "$METHODS" | cut -f1 | grep -v '^$' | while read -r name; do echo "$dataset $name $seed"; done
  done
done > "$LISTS/jobs.txt"

# Scheduler. Every JVM may grow towards its maximum heap (HEAP, 30 GB): G1 commits far more than the live model, so
# memory, not cores, limits the number of parallel jobs. The scheduler runs at most PARALLEL jobs, of which at most
# LARGE_PARALLEL run a method matching LARGE_METHODS (an extended regular expression, e.g., '^SGBT-FIMTDD'; empty = no
# limit), the methods whose models can fill the heap. A job that does not fit is passed over for the next one that does.
# A job starts only while at least MIN_FREE_GB GB of memory (SMALL_MIN_FREE_GB, default MIN_FREE_GB, for a job of
# another method) are available (Linux; 0 = no check) and START_GAP seconds
# after the previous start, so that the memory of the new JVM, which grows mostly in its first minutes, is seen before
# the next start. If the available memory falls below EMERGENCY_FREE_GB GB (0 = never), the most recently started job is
# stopped (its run is not recorded, and run_sgbt.sh logs exit code 143) and queued again, and the number of parallel
# jobs is lowered to the number of jobs still running, so that the Linux OOM killer never has to stop a process. The
# limit is raised again by one every RECOVER_MIN minutes (default 30) without a stop, up to PARALLEL, so that a stop on
# an expensive stream does not slow down the cheaper streams that follow.
LARGE_METHODS="${LARGE_METHODS:-}"; LARGE_PARALLEL="${LARGE_PARALLEL:-$PARALLEL}"; MIN_FREE_GB="${MIN_FREE_GB:-0}"
SMALL_MIN_FREE_GB="${SMALL_MIN_FREE_GB:-$MIN_FREE_GB}"
START_GAP="${START_GAP:-0}"; EMERGENCY_FREE_GB="${EMERGENCY_FREE_GB:-0}"; RECOVER_S=$(( ${RECOVER_MIN:-30} * 60 ))
free_gb() { awk '/^MemAvailable:/ {print int($2 / 1048576)}' /proc/meminfo 2>/dev/null || echo 1000000; }
stop_java() {  # stops the JVMs among the descendants of a process
  local c; for c in $(pgrep -P "$1"); do stop_java "$c"; done
  [ "$(ps -o comm= -p "$1" 2>/dev/null)" = java ] && kill -TERM "$1"
}
pending=()  # the jobs whose run is not recorded yet (a restart skips the others without waiting START_GAP for them)
while read -r dataset name seed; do
  grep -qs "^$dataset,$name,$seed," "$PARTS/${dataset}__${name}__s$seed.csv" || pending+=("$dataset $name $seed")
done < "$LISTS/jobs.txt"
pids=(); large=(); started_jobs=()  # index-aligned, in the order of the starts: running jobs, large or not, job
last_start=-100000; last_stop=-100000; limit=$PARALLEL; last_raise=0
while [ ${#pending[@]} -gt 0 ] || [ ${#pids[@]} -gt 0 ]; do
  for i in "${!pids[@]}"; do
    kill -0 "${pids[$i]}" 2>/dev/null || { wait "${pids[$i]}"; unset 'pids[i]' 'large[i]' 'started_jobs[i]'; }
  done
  free=$(free_gb)
  if [ "$EMERGENCY_FREE_GB" -gt 0 ] && [ "$free" -lt "$EMERGENCY_FREE_GB" ] && [ ${#pids[@]} -gt 1 ] \
     && [ $((SECONDS - last_stop)) -ge 60 ]; then
    idx=("${!pids[@]}"); last=${idx[${#idx[@]} - 1]}
    limit=$((${#pids[@]} - 1))
    echo "$(date '+%H:%M:%S') STOPPED ${started_jobs[$last]} (${free} GB available): queued again, limit=$limit"
    stop_java "${pids[$last]}"; pending=("${started_jobs[$last]}" ${pending[@]+"${pending[@]}"}); last_stop=$SECONDS
  elif [ "$limit" -lt "$PARALLEL" ] && [ $((SECONDS - last_stop)) -ge "$RECOVER_S" ] \
       && [ $((SECONDS - last_raise)) -ge "$RECOVER_S" ]; then
    limit=$((limit + 1)); last_raise=$SECONDS
    echo "$(date '+%H:%M:%S') limit=$limit"
  fi
  running_large=0; for l in ${large[@]+"${large[@]}"}; do running_large=$((running_large + l)); done
  started=0
  # (the queue is scanned only while some job could start: it holds up to tens of thousands of jobs)
  if [ ${#pids[@]} -lt "$limit" ] && [ ${#pending[@]} -gt 0 ] \
     && { [ "$free" -ge "$MIN_FREE_GB" ] || [ "$free" -ge "$SMALL_MIN_FREE_GB" ]; } \
     && [ $((SECONDS - last_start)) -ge "$START_GAP" ] && [ $((SECONDS - last_stop)) -ge 60 ]; then
    for i in "${!pending[@]}"; do
      read -r dataset name seed <<< "${pending[$i]}"
      l=0; [ -n "$LARGE_METHODS" ] && [[ "$name" =~ $LARGE_METHODS ]] && l=1
      [ $l -eq 1 ] && [ "$running_large" -ge "$LARGE_PARALLEL" ] && continue
      need=$MIN_FREE_GB; [ $l -eq 0 ] && [ -n "$LARGE_METHODS" ] && need=$SMALL_MIN_FREE_GB
      [ "$free" -lt "$need" ] && continue
      "$EXP_DIR/scripts/run_sgbt.sh" "$LISTS/method_$name.tsv" "$LISTS/dataset_$dataset.txt" "$seed" \
        "$PARTS/${dataset}__${name}__s$seed.csv" &
      pids+=($!); large+=($l); started_jobs+=("${pending[$i]}"); unset 'pending[i]'; started=1; last_start=$SECONDS
      break
    done
  fi
  sleep $((started ? 1 : 10))
done

# Merge the parts (one header)
first=1
for part in "$PARTS"/*.csv; do
  if [ $first -eq 1 ]; then cat "$part"; first=0; else tail -n +2 "$part"; fi
done > "$OUT_DIR.csv"
echo "merged $(($(wc -l < "$OUT_DIR.csv") - 1)) runs into $OUT_DIR.csv"
