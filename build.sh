#!/usr/bin/env bash
# ============================================================
#  Storm Client - build without Gradle
#
#  Needs nothing but a JDK. No downloads, no wrapper, no
#  network. Produces dist/storm-agent.jar and
#  dist/storm-launcher.jar.
#
#  Usage:  ./build.sh          build everything
#          ./build.sh test     build, then run the smoke test
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"
ROOT="$PWD"
OUT="$ROOT/build-out"
DIST="$ROOT/dist"

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"

command -v "$JAVAC" >/dev/null 2>&1 || { echo "javac not found. Install a JDK or set JAVA_HOME."; exit 1; }

VERSION=$("$JAVAC" -version 2>&1 | sed -E 's/javac ([0-9]+).*/\1/')
echo "using JDK $VERSION  ($JAVAC)"

if [ "$VERSION" -lt 17 ]; then
    echo
    echo "  The launcher needs JDK 17 or newer. Found $VERSION."
    echo "  Core and agent will still be built."
    echo
    BUILD_LAUNCHER=0
else
    BUILD_LAUNCHER=1
fi

rm -rf "$OUT" "$DIST"
mkdir -p "$OUT/core" "$OUT/agent" "$OUT/launcher" "$OUT/test" "$DIST"

# ---- storm-core ---------------------------------------------------
echo "[1/4] storm-core"
find "$ROOT/storm-core/src/main/java" -name '*.java' > "$OUT/core.txt"
"$JAVAC" --release 8 -nowarn -d "$OUT/core" @"$OUT/core.txt"

# ---- storm-agent --------------------------------------------------
echo "[2/4] storm-agent"
find "$ROOT/storm-agent/src/main/java" -name '*.java' > "$OUT/agent.txt"
"$JAVAC" --release 8 -nowarn -cp "$OUT/core" -d "$OUT/agent" @"$OUT/agent.txt"

cat > "$OUT/agent-manifest.txt" <<'MANIFEST'
Premain-Class: xyz.stormclient.agent.StormAgent
Agent-Class: xyz.stormclient.agent.StormAgent
Can-Retransform-Classes: true
Can-Redefine-Classes: true
MANIFEST

cp -r "$OUT/core/." "$OUT/agent/"
"$JAR" --create --file "$DIST/storm-agent.jar" --manifest "$OUT/agent-manifest.txt" -C "$OUT/agent" .

# ---- storm-launcher -----------------------------------------------
if [ "$BUILD_LAUNCHER" = "1" ]; then
    echo "[3/4] storm-launcher"
    find "$ROOT/storm-launcher/src/main/java" -name '*.java' > "$OUT/launcher.txt"
    "$JAVAC" --release 17 -nowarn -cp "$OUT/core" -d "$OUT/launcher" @"$OUT/launcher.txt"

    cp -r "$OUT/core/." "$OUT/launcher/"
    "$JAR" --create --file "$DIST/storm-launcher.jar" \
           --main-class xyz.stormclient.launcher.StormLauncher -C "$OUT/launcher" .
else
    echo "[3/4] storm-launcher  SKIPPED (needs JDK 17+)"
fi

# ---- smoke test ---------------------------------------------------
if [ "${1:-}" = "test" ]; then
    echo "[4/4] smoke test"
    find "$ROOT/storm-core/src/test/java" -name '*.java' > "$OUT/test.txt"
    "$JAVAC" --release 8 -nowarn -cp "$OUT/core" -d "$OUT/test" @"$OUT/test.txt"
    "$JAVA" -cp "$OUT/core:$OUT/test" xyz.stormclient.test.StormSmokeTest
else
    echo "[4/4] smoke test      skipped (run ./build.sh test to include it)"
fi

echo
echo "done. jars are in dist/"
ls -1 "$DIST"
