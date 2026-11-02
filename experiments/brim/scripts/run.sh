#!/bin/bash
# Runs every (dataset, method, seed) combination sequentially (one JVM per run, so that wall times are comparable)
# and appends one CSV line per run to the output file. Runs already in the output file are skipped, so an
# interrupted experiment can be resumed by running the same command again.
#
# Usage: scripts/run.sh <methods.tsv> <datasets.txt> "<seeds>" <output.csv>
#   methods.tsv: one method per line, "<name><TAB><MOA learner CLI>"
#   datasets.txt: one ARFF name (without .arff) per line, read from $DATA_DIR
# A run that reaches TIME_LIMIT (env.sh) is recorded with empty measures; a failed run (no result line) is skipped and
# its log $LOG_DIR/<dataset>.<method>.<seed>.err ends with the JVM's exit code.
set -uo pipefail
source "$(dirname "$0")/env.sh"
METHODS="$1"; DATASETS="$2"; SEEDS="$3"; OUT="$4"
CP="$(classpath)"
LOG_DIR="$EXP_DIR/results/logs"; mkdir -p "$LOG_DIR" "$(dirname "$OUT")"
[ -s "$OUT" ] || "$JAVA" -cp "$CP" PrequentialBenchmark --header > "$OUT"
while read -r dataset; do
  [ -z "$dataset" ] && continue
  while IFS=$'\t' read -r name cli; do
    [ -z "$name" ] && continue
    for seed in $SEEDS; do
      if grep -q "^$dataset,$name,$seed," "$OUT"; then continue; fi
      echo "$(date '+%H:%M:%S') $dataset $name seed=$seed"
      line=$(${LIMIT[@]+"${LIMIT[@]}"} "$JAVA" "${JVM_OPTS[@]}" -cp "$CP" PrequentialBenchmark "$DATA_DIR/$dataset.arff" "$dataset" "$name" "$cli" "$seed" "$MEMORY_SAMPLES" 2>"$LOG_DIR/$dataset.$name.$seed.err" | tail -1; exit "${PIPESTATUS[0]}")
      code=$?
      if [ "$code" -eq 124 ] && [ ${#LIMIT[@]} -gt 0 ]; then
        timeout_row "$OUT" "$dataset" "$name" "$seed" >> "$OUT"
        echo "$(date '+%Y-%m-%d %H:%M:%S') stopped at the time limit of $TIME_LIMIT" >> "$LOG_DIR/$dataset.$name.$seed.err"
        mv "$LOG_DIR/$dataset.$name.$seed.err" "$LOG_DIR/$dataset.$name.$seed.timeout"
        echo "  TIMEOUT ($TIME_LIMIT)"
      elif [[ "$line" == "$dataset,$name,$seed,"* ]]; then
        echo "$line" >> "$OUT"
        # Keep the notes of successful runs (e.g., a model measured in parts), drop empty files
        if [ -s "$LOG_DIR/$dataset.$name.$seed.err" ]; then
          mv "$LOG_DIR/$dataset.$name.$seed.err" "$LOG_DIR/$dataset.$name.$seed.notes"
        else
          rm -f "$LOG_DIR/$dataset.$name.$seed.err"
        fi
      else
        status="exit code $code"; [ "$code" -gt 128 ] && status+=" (signal $((code - 128)))"
        echo "$(date '+%Y-%m-%d %H:%M:%S') $status" >> "$LOG_DIR/$dataset.$name.$seed.err"
        echo "  FAILED: $status (see $LOG_DIR/$dataset.$name.$seed.err)"
      fi
    done
  done < <(grep -v '^#' "$METHODS")
done < <(grep -v '^#' "$DATASETS")
