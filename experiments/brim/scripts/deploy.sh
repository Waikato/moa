#!/bin/bash
# Copies what an experiment needs to a server without Maven: the compiled MOA classes (moa/target/classes, Java 8
# bytecode), the dependency jars of build/cp.txt and the SizeOf agent (into lib/), the compiled runners, the scripts,
# configs, analysis and the given datasets. Writes a build/cp.txt with the server paths. The remote layout mirrors the
# repository, so env.sh finds MOA at <remote dir>/moa:
#   <remote dir>/moa/target/classes, <remote dir>/lib/*.jar, <remote dir>/experiments/brim/{scripts,configs,...}
# Run scripts/build.sh first. Results on the server are never touched; the SizeOf agent is at <remote dir>/lib/.
#
# Usage: scripts/deploy.sh <user@host> <remote dir> <dataset dir or ARFF file, relative to experiments/brim>...
#   e.g. scripts/deploy.sh nuwgun@lyon.hh.se /data/home/nuwgun/brim_ensemble_regression RDatasets
# Environment: SSH_PORT (default 20022).
set -euo pipefail
source "$(dirname "$0")/env.sh"
[ $# -ge 3 ] || { sed -n '2,11p' "$0"; exit 1; }
HOST="$1"; REMOTE="$2"; shift 2
RSYNC=(rsync -az -e "ssh -p ${SSH_PORT:-20022}")
cd "$EXP_DIR"
ssh -p "${SSH_PORT:-20022}" "$HOST" "mkdir -p $REMOTE/lib $REMOTE/moa/target $REMOTE/experiments/brim/build/classes"

# Dependency jars and the SizeOf agent, and the classpath with the server paths
JARS=$(tr ':' ' ' < "$BUILD_DIR/cp.txt")
"${RSYNC[@]}" $JARS "$SIZEOF_AGENT" "$HOST:$REMOTE/lib/"
for jar in $JARS; do echo -n "$REMOTE/lib/$(basename "$jar"):"; done | sed 's/:$//' > "$BUILD_DIR/cp_server.txt"
"${RSYNC[@]}" "$BUILD_DIR/cp_server.txt" "$HOST:$REMOTE/experiments/brim/build/cp.txt"

"${RSYNC[@]}" --delete "$MOA_MODULE/target/classes/" "$HOST:$REMOTE/moa/target/classes/"
"${RSYNC[@]}" "$BUILD_DIR/classes/" "$HOST:$REMOTE/experiments/brim/build/classes/"
"${RSYNC[@]}" --exclude __pycache__ scripts src configs analysis README.md "$HOST:$REMOTE/experiments/brim/"
"${RSYNC[@]}" -R "$@" "$HOST:$REMOTE/experiments/brim/"
echo "Deployed to $HOST:$REMOTE (SizeOf agent: $REMOTE/lib/$(basename "$SIZEOF_AGENT"))"
