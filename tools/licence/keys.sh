#!/usr/bin/env bash
# ============================================================
#  Day to day licence admin, over the server's API.
#
#  Set these once, e.g. in ~/.bashrc:
#      export STORM_SERVER=https://keys.example.com
#      export STORM_TOKEN=<the adminToken from the server config>
#
#  Then:
#      ./keys.sh issue "ivan@example.com" pro 30
#      ./keys.sh list
#      ./keys.sh status STORM-4K7M-9QX2-JH3D
#      ./keys.sh revoke STORM-4K7M-9QX2-JH3D
#      ./keys.sh restore STORM-4K7M-9QX2-JH3D
#      ./keys.sh unbind STORM-4K7M-9QX2-JH3D
# ============================================================
set -euo pipefail

SERVER="${STORM_SERVER:-}"
TOKEN="${STORM_TOKEN:-}"

[ -n "$SERVER" ] || { echo "set STORM_SERVER first"; exit 1; }

# every call but 'status' needs the admin token
admin() {
    [ -n "$TOKEN" ] || { echo "set STORM_TOKEN first"; exit 1; }
    curl -fsS -X POST "$SERVER/api/$1" \
         -H "Authorization: Bearer $TOKEN" \
         -H "Content-Type: application/json" \
         -d "$2"
    echo
}

case "${1:-}" in
    issue)
        HOLDER="${2:?who is it for}"
        PLAN="${3:-pro}"
        DAYS="${4:-0}"
        admin issue "{\"holder\":\"$HOLDER\",\"plan\":\"$PLAN\",\"days\":$DAYS}"
        ;;
    revoke)  admin revoke "{\"key\":\"${2:?which key}\"}" ;;
    restore) admin revoke "{\"key\":\"${2:?which key}\",\"revoked\":\"false\"}" ;;
    unbind)  admin unbind "{\"key\":\"${2:?which key}\"}" ;;
    list)
        [ -n "$TOKEN" ] || { echo "set STORM_TOKEN first"; exit 1; }
        curl -fsS "$SERVER/api/keys" -H "Authorization: Bearer $TOKEN"
        echo
        ;;
    status)
        curl -fsS "$SERVER/api/status?key=${2:?which key}"
        echo
        ;;
    *)
        sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
