#!/usr/bin/env bash
# End-to-end smoke test: install APK in the running emulator, configure
# webhook against a local mock ntfy server, post 3 notifications offline,
# turn networking back on, and verify all 3 arrive in order.
#
# Requires: emulator already booted, adb on PATH.

set -euo pipefail

PORT=${PORT:-8765}
LOG=$(mktemp -t ntfy-XXXXXX.log)
APK=app/build/outputs/apk/debug/app-debug.apk
PKG=pl.d33.notificationlog.ntfy
# The Listener class kept upstream's Kotlin namespace; only applicationId changed.
LISTENER="$PKG/de.jl.notificationlog.service.NotificationListenerService"
URL="http://10.0.2.2:$PORT/topic"

[[ -f "$APK" ]] || { echo "APK not built — run ./gradlew :app:assembleDebug" >&2; exit 1; }

# `cmd notification post` is API 30+. Older devices need a helper app to fire
# a real notification, which is out of scope for this script.
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
if [[ "$SDK" -lt 30 ]]; then
    echo "smoke-test.sh needs API 30+ for 'cmd notification post' (device is API $SDK)." >&2
    echo "Use connectedAndroidTest for the in-process E2E coverage instead." >&2
    exit 2
fi

cleanup() {
    [[ -n "${SRV_PID:-}" ]] && kill "$SRV_PID" 2>/dev/null || true
    rm -f "$LOG"
}
trap cleanup EXIT

python3 "$(dirname "$0")/mock-ntfy-server.py" "$PORT" "$LOG" &
SRV_PID=$!
sleep 1

echo "[1/7] installing APK"
adb install -r -g "$APK" >/dev/null
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

echo "[2/7] enabling notification listener"
adb shell cmd notification allow_listener "$LISTENER"

echo "[3/7] writing webhook config to SharedPreferences"
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/${PKG}_preferences.xml'" <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="webhook_enabled" value="true" />
    <string name="webhook_url">$URL</string>
    <string name="webhook_bearer_token">smoketoken</string>
</map>
EOF
adb shell am force-stop "$PKG"
# launch the app once so it picks up prefs and registers WorkManager
adb shell monkey -p "$PKG" 1 >/dev/null
sleep 2

echo "[4/7] posting first notification (online — sanity check)"
adb shell cmd notification post -t 'sanity' tag1 'hello-online'
sleep 5
grep -q 'hello-online' "$LOG" || { echo "online delivery failed:"; cat "$LOG"; exit 1; }
echo "  -> ok, log so far:"; cat "$LOG"

echo "[5/7] going offline + posting 3 notifications"
adb shell svc wifi disable
adb shell svc data disable
truncate -s 0 "$LOG"  # reset
for i in 1 2 3; do
    adb shell cmd notification post -t "offline-$i" "tag-$i" "body-$i"
done
sleep 4
if [[ -s "$LOG" ]]; then
    echo "FAIL: log is non-empty while offline:"; cat "$LOG"; exit 1
fi
echo "  -> good, server got nothing while offline"

echo "[6/7] turning network back on"
adb shell svc wifi enable
adb shell svc data enable
# WorkManager backoff starts at 30s; wait up to 90s for all 3 to arrive
for _ in $(seq 1 18); do
    n=$(wc -l < "$LOG")
    [[ "$n" -ge 3 ]] && break
    sleep 5
done
echo "  -> log:"; cat "$LOG"
n=$(wc -l < "$LOG")
[[ "$n" -ge 3 ]] || { echo "FAIL: only $n/3 delivered"; exit 1; }

# Order check
titles=$(jq -r '.body' < "$LOG")
expected=$'body-1\nbody-2\nbody-3'
[[ "$titles" == "$expected" ]] || { echo "FAIL: order mismatch. got:"; echo "$titles"; exit 1; }

echo "[7/7] PASS — all 3 deliveries arrived in order"
