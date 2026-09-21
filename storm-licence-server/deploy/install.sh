#!/usr/bin/env bash
# ============================================================
#  Sets up the Storm licence server on Ubuntu.
#
#  Run as root on a fresh VPS:
#      bash install.sh
#
#  It creates a service account and /opt/storm-licences, puts the
#  jar, config and systemd unit in place, and stops there. It does
#  not start anything: the config still needs an admin token and a
#  signing key, and it prints what to do next.
# ============================================================
set -euo pipefail

DIR=/opt/storm-licences
USER=storm
HERE="$(cd "$(dirname "$0")" && pwd)"

[ "$(id -u)" -eq 0 ] || { echo "run this as root"; exit 1; }

command -v java >/dev/null 2>&1 || {
    echo "Java is not installed. Install it first:"
    echo "    apt update && apt install -y openjdk-17-jre-headless"
    exit 1
}

id -u "$USER" >/dev/null 2>&1 || useradd --system --home "$DIR" --shell /usr/sbin/nologin "$USER"
mkdir -p "$DIR"

copy() {
    if [ -e "$DIR/$2" ]; then
        echo "  keeping existing $DIR/$2"
    else
        cp "$1" "$DIR/$2"
        echo "  wrote $DIR/$2"
    fi
}

echo "installing into $DIR"
[ -f "$HERE/../../dist/storm-licence-server.jar" ] \
    && cp "$HERE/../../dist/storm-licence-server.jar" "$DIR/storm-licence-server.jar" \
    && echo "  wrote $DIR/storm-licence-server.jar" \
    || echo "  no jar found, copy dist/storm-licence-server.jar here yourself"
copy "$HERE/storm-licences.properties" storm-licences.properties

cp "$HERE/storm-licences.service" /etc/systemd/system/storm-licences.service
systemctl daemon-reload
echo "  wrote /etc/systemd/system/storm-licences.service"

chown -R "$USER:$USER" "$DIR"
chmod 750 "$DIR"
chmod 640 "$DIR/storm-licences.properties"
# the signing key and the key store are the two files worth stealing
[ -f "$DIR/licence-private.key" ] && chmod 600 "$DIR/licence-private.key"
[ -f "$DIR/licences.json" ] && chmod 600 "$DIR/licences.json"

TOKEN=$(head -c 32 /dev/urandom | base64)

cat <<NEXT

done. three things left:

  1. Put your signing key in place and lock it down:
         cp licence-private.key $DIR/
         chown $USER:$USER $DIR/licence-private.key
         chmod 600 $DIR/licence-private.key

  2. Set an admin token in $DIR/storm-licences.properties.
     Here is one:
         adminToken = $TOKEN

  3. Start it, then put nginx in front for TLS:
         systemctl enable --now storm-licences
         journalctl -u storm-licences -f

NEXT
