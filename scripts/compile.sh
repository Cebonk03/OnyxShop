#!/bin/bash
# Fast javac check of the main and apistubs sources (also used by scripts/hooks/pre-commit).
# usage: compile.sh <out-dir> [logfile]
set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT" || exit 1

OUT=${1:-/tmp/onyxshop-classes}
LOG=${2:-/tmp/onyxshop-javac.log}


CP=$( { find libs/cp libs/api -name '*.jar'; } 2>/dev/null | tr '\n' ':' )

mkdir -p "$OUT"
SOURCES=$(find src/main/java src/apistubs/java -name '*.java')
echo "compiling $(echo "$SOURCES" | wc -l) sources into $OUT"

javac -J-Xmx2g -nowarn -proc:none -source 8 -target 8 -encoding UTF-8 -Xlint:-options -Xmaxerrs 20000 -cp "$CP" -d "$OUT" $SOURCES 2> "$LOG"
RC=$?
ERRORS=$(grep -c 'error:' "$LOG" || true)
echo "errors: $ERRORS"
if [ "$RC" -ne 0 ] || [ "$ERRORS" -ne 0 ]; then
  exit 1
fi
exit 0
