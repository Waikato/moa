#!/bin/bash
# Compiles MOA (module moa/) and the benchmark runners (src/*.java), and records the classpath. The runners are compiled
# for JAVAC_RELEASE (default 21, the Java version of the servers), so the same classes run locally and on a server.
set -euo pipefail
source "$(dirname "$0")/env.sh"
mkdir -p "$BUILD_DIR/classes"
(cd "$MOA_MODULE" && mvn -q -o compile && mvn -q -o dependency:build-classpath -Dmdep.outputFile="$BUILD_DIR/cp.txt")
javac -nowarn --release "${JAVAC_RELEASE:-21}" -d "$BUILD_DIR/classes" \
  -cp "$MOA_MODULE/target/classes:$(cat "$BUILD_DIR/cp.txt")" "$EXP_DIR"/src/*.java
echo "Built. Java: $("$JAVA" -version 2>&1 | head -1)"
