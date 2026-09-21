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
#          ./build.sh preview  also paint the menu and HUD into preview/*.png
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

# "javac 1.8.0_504" and "javac 21.0.10" both have to land on a plain number
RAW=$("$JAVAC" -version 2>&1 | grep -oE 'javac [0-9][0-9._]*' | head -1 | awk '{print $2}')
VERSION=${RAW%%.*}
if [ "$VERSION" = "1" ]; then VERSION=$(echo "$RAW" | cut -d. -f2); fi
case "$VERSION" in
    ''|*[!0-9]*) echo "could not read the javac version from: $RAW"; exit 1 ;;
esac
echo "using JDK $VERSION  ($JAVAC)"

if [ "$VERSION" -lt 17 ]; then
    echo
    echo "  This build needs JDK 17 or newer, this one is $VERSION."
    echo "  Point JAVA_HOME at a current JDK and run again:"
    echo "    export JAVA_HOME=/path/to/jdk-21"
    echo
    exit 1
fi

# dist keeps whatever else is in it, in particular the bridge jar, which costs
# a whole ForgeGradle build to replace
rm -rf "$OUT"
mkdir -p "$OUT/core" "$OUT/agent" "$OUT/launcher" "$OUT/server" "$OUT/test" "$DIST"

# ---- storm-core ---------------------------------------------------
echo "[1/5] storm-core"
find "$ROOT/storm-core/src/main/java" -name '*.java' > "$OUT/core.txt"
"$JAVAC" --release 8 -encoding UTF-8 -nowarn -d "$OUT/core" @"$OUT/core.txt"

# ---- storm-agent --------------------------------------------------
echo "[2/5] storm-agent"
find "$ROOT/storm-agent/src/main/java" -name '*.java' > "$OUT/agent.txt"
"$JAVAC" --release 8 -encoding UTF-8 -nowarn -cp "$OUT/core" -d "$OUT/agent" @"$OUT/agent.txt"

cat > "$OUT/agent-manifest.txt" <<'MANIFEST'
Premain-Class: xyz.stormclient.agent.StormAgent
Agent-Class: xyz.stormclient.agent.StormAgent
Can-Retransform-Classes: true
Can-Redefine-Classes: true
MANIFEST

cp -r "$OUT/core/." "$OUT/agent/"
"$JAR" --create --file "$DIST/storm-agent.jar" --manifest "$OUT/agent-manifest.txt" -C "$OUT/agent" .

# ---- storm-launcher -----------------------------------------------
echo "[3/5] storm-launcher"
find "$ROOT/storm-launcher/src/main/java" -name '*.java' > "$OUT/launcher.txt"
"$JAVAC" --release 17 -encoding UTF-8 -nowarn -cp "$OUT/core" -d "$OUT/launcher" @"$OUT/launcher.txt"

cp -r "$OUT/core/." "$OUT/launcher/"
"$JAR" --create --file "$DIST/storm-launcher.jar" \
       --main-class xyz.stormclient.launcher.StormLauncher -C "$OUT/launcher" .

# ---- storm-licence-server -----------------------------------------
echo "[4/5] storm-licence-server"
find "$ROOT/storm-licence-server/src/main/java" -name '*.java' > "$OUT/server.txt"
"$JAVAC" --release 17 -encoding UTF-8 -nowarn -cp "$OUT/core" -d "$OUT/server" @"$OUT/server.txt"

cp -r "$OUT/core/." "$OUT/server/"
"$JAR" --create --file "$DIST/storm-licence-server.jar" \
       --main-class xyz.stormclient.licenceserver.LicenceServer -C "$OUT/server" .

# ---- smoke test ---------------------------------------------------
if [ "${1:-}" = "test" ] || [ "${1:-}" = "preview" ]; then
    echo "[5/5] smoke test"
    find "$ROOT/storm-core/src/test/java" -name '*.java' > "$OUT/test.txt"
    "$JAVAC" --release 8 -encoding UTF-8 -nowarn -cp "$OUT/core" -d "$OUT/test" @"$OUT/test.txt"
    "$JAVA" -cp "$OUT/core:$OUT/test" xyz.stormclient.test.StormSmokeTest
    "$JAVA" -cp "$OUT/core:$OUT/test" xyz.stormclient.test.preview.ClickTest

    # paints the real menu into a PNG, so its layout can be checked without a game
    if [ "${1:-}" = "preview" ]; then
        mkdir -p "$ROOT/preview"
        for page in Combat Theme Settings Configs Keybinds Licence; do
            "$JAVA" -cp "$OUT/core:$OUT/test" xyz.stormclient.test.preview.GuiPreview \
                    960 540 "$ROOT/preview/menu-$page.png" "menu:$page"
        done
        for size in "960 540" "480 270"; do
            set -- $size
            "$JAVA" -cp "$OUT/core:$OUT/test" xyz.stormclient.test.preview.GuiPreview \
                    "$1" "$2" "$ROOT/preview/panels-$1x$2.png" panels
        done
        "$JAVA" -cp "$OUT/core:$OUT/test" xyz.stormclient.test.preview.GuiPreview \
                960 540 "$ROOT/preview/hud-960x540.png" hud
    fi
else
    echo "[5/5] smoke test      skipped (run ./build.sh test to include it)"
fi

echo
echo "done. jars are in dist/"
ls -1 "$DIST"
