#!/bin/bash
# Classification counterpart of run.sh for the SGBT experiment: runs every (dataset, method, seed) combination
# sequentially with PrequentialClassificationBenchmark (one JVM per run) and appends one CSV line per run to the output
# file. Runs already in the output file are skipped, so an interrupted experiment can be resumed with the same command.
#
# Usage: scripts/run_sgbt.sh <methods.tsv> <datasets.txt> "<seeds>" <output.csv>
#   methods.tsv: one method per line, "<name><TAB><MOA learner CLI>"
#   datasets.txt: one ARFF name (without .arff) per line, read from $DATA_DIR
#
# SGBT trains the one-vs-rest models of multi-class streams and predicts with its boosting steps in parallel streams
# (Java's common fork-join pool). FJ_PARALLELISM (default 0: no pool threads, everything runs in the calling thread)
# limits this, so that each run uses one core, wall times do not depend on the other jobs, and runs can be parallelized
# across jobs instead. For the same reason GC_THREADS (default 2) limits the parallel GC threads of each JVM (by default
# the JVM starts one per core, which overloads a server running many jobs).
#
# A run that reaches TIME_LIMIT (env.sh) is recorded with empty measures. A failed run (no result line) is skipped; its
# log $LOG_DIR/<dataset>.<method>.<seed>.err ends with the JVM's exit code, which is above 128 for a death by signal
# (e.g., 137 = SIGKILL, as sent by the Linux OOM killer).
set -uo pipefail
source "$(dirname "$0")/env.sh"
METHODS="$1"; DATASETS="$2"; SEEDS="$3"; OUT="$4"
CP="$(classpath)"
JVM_OPTS+=(-Djava.util.concurrent.ForkJoinPool.common.parallelism="${FJ_PARALLELISM:-0}"
  -XX:ParallelGCThreads="${GC_THREADS:-2}" -XX:ConcGCThreads=1)
LOG_DIR="$EXP_DIR/results/logs"; mkdir -p "$LOG_DIR" "$(dirname "$OUT")"
[ -s "$OUT" ] || "$JAVA" -cp "$CP" PrequentialClassificationBenchmark --header 2>/dev/null | tail -1 > "$OUT"
while read -r dataset; do
  [ -z "$dataset" ] && continue
  while IFS=$'\t' read -r name cli; do
    [ -z "$name" ] && continue
    for seed in $SEEDS; do
      if grep -q "^$dataset,$name,$seed," "$OUT"; then continue; fi
      echo "$(date '+%H:%M:%S') $dataset $name seed=$seed"
      err="$LOG_DIR/$dataset.$name.$seed.err"
      line=$(${LIMIT[@]+"${LIMIT[@]}"} "$JAVA" "${JVM_OPTS[@]}" -cp "$CP" PrequentialClassificationBenchmark "$DATA_DIR/$dataset.arff" "$dataset" "$name" "$cli" "$seed" "$MEMORY_SAMPLES" 2>"$err" | tail -1; exit "${PIPESTATUS[0]}")
      code=$?
      if [ "$code" -eq 124 ] && [ ${#LIMIT[@]} -gt 0 ]; then
        timeout_row "$OUT" "$dataset" "$name" "$seed" >> "$OUT"
        echo "$(date '+%Y-%m-%d %H:%M:%S') stopped at the time limit of $TIME_LIMIT" >> "$err"
        mv "$err" "${err%.err}.timeout"
        echo "  TIMEOUT ($TIME_LIMIT)"
      elif [[ "$line" == "$dataset,$name,$seed,"* ]]; then
        echo "$line" >> "$OUT"; rm -f "$err"
      else
        # An exit code above 128 is a death by signal 128 + n, e.g., 137 = SIGKILL (the Linux OOM killer, or kill -9)
        status="exit code $code"; [ "$code" -gt 128 ] && status+=" (signal $((code - 128)))"
        echo "$(date '+%Y-%m-%d %H:%M:%S') $status" >> "$err"
        echo "  FAILED: $status (see $err)"
      fi
    done
  done < <(grep -v '^#' "$METHODS")
done < <(grep -v '^#' "$DATASETS")
