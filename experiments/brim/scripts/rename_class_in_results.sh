#!/bin/bash
# Replaces the old class name PLASTICRegressor by BRIMRegressor in result and configuration files, e.g., after copying
# the results of the ensemble experiments from a server that ran them before the class was renamed. Only the text is
# changed; no file is removed or added.
#
# Usage: scripts/rename_class_in_results.sh [--dry-run] [path ...]
#   paths default to results/, configs/ and paper/tables/ of the experiment folder;
#   --dry-run lists the files and the number of occurrences without changing them.
set -uo pipefail
EXP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OLD="PLASTICRegressor"; NEW="BRIMRegressor"
DRY_RUN=0
if [ "${1:-}" = "--dry-run" ]; then DRY_RUN=1; shift; fi
if [ $# -gt 0 ]; then paths=("$@"); else paths=("$EXP_DIR/results" "$EXP_DIR/configs" "$EXP_DIR/paper/tables"); fi

changed=0
while IFS= read -r file; do
  count=$(grep -o "$OLD" "$file" | wc -l | tr -d ' ')
  echo "  $file: $count"
  # perl -pi works the same on Linux and macOS (unlike sed -i)
  [ $DRY_RUN -eq 0 ] && perl -pi -e "s/$OLD/$NEW/g" "$file"
  changed=$((changed + 1))
done < <(grep -rlI "$OLD" "${paths[@]}" 2>/dev/null)

if [ $changed -eq 0 ]; then
  echo "No occurrences of $OLD found."
elif [ $DRY_RUN -eq 1 ]; then
  echo "$changed files contain $OLD (nothing changed: --dry-run)."
else
  echo "Replaced $OLD by $NEW in $changed files."
fi
