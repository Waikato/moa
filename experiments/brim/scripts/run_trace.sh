#!/bin/bash
# Motivating experiment: memory and cumulative wall time along the stream (FriedmanGra, seed 1, every 2,000 instances)
# of FIMT-DD, of the initial regression adaptation of PLASTIC (variance reduction + E-BST), of the same with
# quantization observers, and of BRIM. Writes results/raw/trace.csv.
set -uo pipefail
source "$(dirname "$0")/env.sh"
CP="$(classpath)"; OUT="$EXP_DIR/results/raw/trace.csv"; DATASET="${TRACE_DATASET:-FriedmanGra}"
"$JAVA" -cp "$CP" TraceBenchmark --header > "$OUT"
while IFS=$'\t' read -r name cli; do
  [ -z "$name" ] && continue
  echo "$(date '+%H:%M:%S') $DATASET $name"
  "$JAVA" "${JVM_OPTS[@]}" -cp "$CP" TraceBenchmark "$DATA_DIR/$DATASET.arff" "$DATASET" "$name" "$cli" 1 2000 2>/dev/null | grep "^$DATASET," >> "$OUT"
done < <(grep -v '^#' "$EXP_DIR/configs/trace_methods.tsv")
