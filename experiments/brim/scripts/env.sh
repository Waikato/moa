#!/bin/bash
# Paths and JVM settings shared by all scripts. Override any of them in the environment, e.g.
#   DATA_DIR=/path/to/RDatasets SIZEOF_AGENT=/path/to/sizeofag-1.1.0.jar scripts/run_main.sh
EXP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOA_MODULE="${MOA_MODULE:-$(cd "$EXP_DIR/../../moa" && pwd)}"
DATA_DIR="${DATA_DIR:-$EXP_DIR/RDatasets}"
SIZEOF_AGENT="${SIZEOF_AGENT:-$HOME/Desktop/CODE/sizeofag-1.1.0.jar}"
# Fall back to the agent in the local Maven repository (e.g., on a server)
[ -f "$SIZEOF_AGENT" ] || SIZEOF_AGENT="$HOME/.m2/repository/com/github/fracpete/sizeofag/1.1.0/sizeofag-1.1.0.jar"
JAVA="${JAVA:-java}"
HEAP="${HEAP:-8g}"
MEMORY_SAMPLES="${MEMORY_SAMPLES:-20}"
BUILD_DIR="$EXP_DIR/build"
# The SizeOf agent needs access to JDK internals (e.g., the fields of ArrayList or TreeMap) on recent JDKs;
# without these --add-opens it silently undercounts.
JVM_OPTS=(-Xmx"$HEAP" -javaagent:"$SIZEOF_AGENT"
  --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED
  --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED)
classpath() { echo "$BUILD_DIR/classes:$MOA_MODULE/target/classes:$(cat "$BUILD_DIR/cp.txt")"; }
