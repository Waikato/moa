#!/bin/bash
# Compiles MOA (module moa/) and the benchmark runner, and records the classpath.
set -euo pipefail
source "$(dirname "$0")/env.sh"
mkdir -p "$BUILD_DIR/classes"
(cd "$MOA_MODULE" && mvn -q -o compile && mvn -q -o dependency:build-classpath -Dmdep.outputFile="$BUILD_DIR/cp.txt")
javac -nowarn -d "$BUILD_DIR/classes" -cp "$MOA_MODULE/target/classes:$(cat "$BUILD_DIR/cp.txt")" "$EXP_DIR/src/PrequentialBenchmark.java" "$EXP_DIR/src/TraceBenchmark.java"
echo "Built. Java: $("$JAVA" -version 2>&1 | head -1)"
