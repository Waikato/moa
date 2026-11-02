#!/bin/bash
# Paths and JVM settings shared by all scripts. Override any of them in the environment, e.g.
#   DATA_DIR=/path/to/RDatasets SIZEOF_AGENT=/path/to/sizeofag-1.1.0.jar scripts/run_main.sh
EXP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOA_MODULE="${MOA_MODULE:-$(cd "$EXP_DIR/../../moa" && pwd)}"
DATA_DIR="${DATA_DIR:-$EXP_DIR/RDatasets}"
SIZEOF_AGENT="${SIZEOF_AGENT:-$HOME/Desktop/CODE/sizeofag-1.1.0.jar}"
# Fall back to the agent copied by scripts/deploy.sh (on a server), then to the local Maven repository
[ -f "$SIZEOF_AGENT" ] || SIZEOF_AGENT="$EXP_DIR/../../lib/sizeofag-1.1.0.jar"
[ -f "$SIZEOF_AGENT" ] || SIZEOF_AGENT="$HOME/.m2/repository/com/github/fracpete/sizeofag/1.1.0/sizeofag-1.1.0.jar"
JAVA="${JAVA:-java}"
# One maximum heap for all experiments: large enough for the biggest ensembles (and for measuring them with the SizeOf
# agent), and below 32 GB so that the JVM keeps compressed object pointers and model sizes stay comparable.
HEAP="${HEAP:-30g}"
# Memory is measured once, at the end of the stream (the final model size); 0 disables the measurement.
MEMORY_SAMPLES="${MEMORY_SAMPLES:-1}"
# Wall-clock limit of one run (a duration for GNU timeout, e.g., 1h; 0 = none). A run that reaches it is recorded with
# empty measures (see timeout_row), so that it is not retried on resume and ranks last in the tuning.
TIME_LIMIT="${TIME_LIMIT:-0}"
BUILD_DIR="$EXP_DIR/build"
# The SizeOf agent needs access to JDK internals (e.g., the fields of ArrayList or TreeMap) on recent JDKs;
# without these --add-opens it silently undercounts.
JVM_OPTS=(-Xmx"$HEAP" -javaagent:"$SIZEOF_AGENT"
  --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED
  --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED)
LIMIT=(); [ "$TIME_LIMIT" != 0 ] && LIMIT=(timeout -k 60 "$TIME_LIMIT")
# CSV line of a run stopped at the time limit: dataset, method and seed, and empty measures (as many as in the header).
timeout_row() {  # <output.csv> <dataset> <method> <seed>
  local fields; fields=$(head -1 "$1" | tr -cd ',' | wc -c)
  printf '%s,%s,%s' "$2" "$3" "$4"; for _ in $(seq 3 "$fields"); do printf ','; done; echo
}
classpath() { echo "$BUILD_DIR/classes:$MOA_MODULE/target/classes:$(cat "$BUILD_DIR/cp.txt")"; }
