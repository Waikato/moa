#!/bin/bash
# Runs every (dataset, method, seed) combination sequentially (one JVM per run, so that wall times are comparable)
# and appends one CSV line per run to the output file. Runs already in the output file are skipped, so an
# interrupted experiment can be resumed by running the same command again.
#
# Usage: scripts/run.sh <methods.tsv> <datasets.txt> "<seeds>" <output.csv>
#   methods.tsv: one method per line, "<name><TAB><MOA learner CLI>"
#   datasets.txt: one ARFF name (without .arff) per line, read from $DATA_DIR
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
      line=$("$JAVA" "${JVM_OPTS[@]}" -cp "$CP" PrequentialBenchmark "$DATA_DIR/$dataset.arff" "$dataset" "$name" "$cli" "$seed" "$MEMORY_SAMPLES" 2>"$LOG_DIR/$dataset.$name.$seed.err" | tail -1)
      if [[ "$line" == "$dataset,$name,$seed,"* ]]; then
        echo "$line" >> "$OUT"
        # Keep the notes of successful runs (e.g., a model measured in parts), drop empty files
        if [ -s "$LOG_DIR/$dataset.$name.$seed.err" ]; then
          mv "$LOG_DIR/$dataset.$name.$seed.err" "$LOG_DIR/$dataset.$name.$seed.notes"
        else
          rm -f "$LOG_DIR/$dataset.$name.$seed.err"
        fi
      else
        echo "  FAILED (see $LOG_DIR/$dataset.$name.$seed.err)"
      fi
    done
  done < <(grep -v '^#' "$METHODS")
done < <(grep -v '^#' "$DATASETS")
