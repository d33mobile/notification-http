#!/usr/bin/env bash
# One-shot bootstrap: mint two signing keys (APK release + F-Droid repo index),
# upload them and their passwords to GitHub Actions secrets for d33mobile/notification-http.
#
# Re-running this script regenerates BOTH keys. F-Droid clients identify the
# upstream repo by its signing certificate fingerprint, and Android refuses to
# upgrade an installed APK signed with a different cert. So:
#
#   * If the secrets are already set: this script will overwrite them and break
#     the upgrade chain on every device that already installed an older build.
#   * Run it exactly once for a given app, store the local copies in a password
#     manager as a recovery backup, and never touch it again.

set -euo pipefail

REPO="d33mobile/notification-http"
WORK=$(mktemp -d -t fdroid-bootstrap-XXXXXX)
trap 'rm -rf "$WORK"' EXIT

cd "$WORK"

require() { command -v "$1" >/dev/null || { echo "missing: $1" >&2; exit 1; } }
require keytool
require fdroid
require gh
require base64
require openssl

gh auth status >/dev/null || { echo "run 'gh auth login' first" >&2; exit 1; }

# fdroid init ERRORs (but keeps going) when ANDROID_HOME is unset. We don't actually
# need the SDK for keystore generation, but silencing the noise also lets the script
# fail loudly on real errors.
export ANDROID_HOME="${ANDROID_HOME:-/mnt/HC_Volume_103952790/android/sdk}"

KEYSTORE_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)
FDROID_PWD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)

echo "[1/6] generating APK upload keystore (RSA 4096, 27-year validity)"
keytool -genkeypair \
    -keystore release.jks \
    -alias upload \
    -keyalg RSA -keysize 4096 \
    -validity 10000 \
    -storepass "$KEYSTORE_PWD" \
    -keypass "$KEYSTORE_PWD" \
    -dname "CN=NotificationLog ntfy fork, OU=fork, O=d33mobile, L=PL, S=PL, C=PL"
RELEASE_KEY_FP=$(keytool -list -keystore release.jks -alias upload \
                         -storepass "$KEYSTORE_PWD" -v 2>/dev/null \
                | awk '/SHA256:/ {print $2; exit}')
echo "    release key SHA256: $RELEASE_KEY_FP"

echo "[2/6] initializing F-Droid repo signing key"
# fdroid init prompts for: keystore pwd, key pwd (same), DName fields. We don't want
# interactive — generate the PKCS12 directly with keytool, then pass --keystore in
# the workflow. fdroid update will load it via the alias + password from env.
keytool -genkeypair \
    -keystore fdroid.p12 \
    -storetype PKCS12 \
    -alias repo_signing_key \
    -keyalg RSA -keysize 4096 \
    -validity 10000 \
    -storepass "$FDROID_PWD" \
    -keypass "$FDROID_PWD" \
    -dname "CN=NotificationLog ntfy fork F-Droid repo, OU=fork, O=d33mobile, L=PL, S=PL, C=PL"
FDROID_KEY_FP=$(keytool -list -keystore fdroid.p12 -alias repo_signing_key \
                        -storetype PKCS12 -storepass "$FDROID_PWD" -v 2>/dev/null \
               | awk '/SHA256:/ {print $2; exit}')
echo "    fdroid key SHA256:  $FDROID_KEY_FP"

echo "[3/6] base64-encoding both keystores"
RELEASE_B64=$(base64 -w0 release.jks)
FDROID_B64=$(base64 -w0 fdroid.p12)

echo "[4/6] uploading 7 GitHub Action secrets to $REPO"
gh secret set RELEASE_KEYSTORE_BASE64   --repo "$REPO" --body "$RELEASE_B64"
gh secret set RELEASE_KEYSTORE_PASSWORD --repo "$REPO" --body "$KEYSTORE_PWD"
gh secret set RELEASE_KEY_ALIAS         --repo "$REPO" --body "upload"
gh secret set RELEASE_KEY_PASSWORD      --repo "$REPO" --body "$KEYSTORE_PWD"
gh secret set FDROID_KEYSTORE_BASE64    --repo "$REPO" --body "$FDROID_B64"
gh secret set FDROID_KEYSTORE_PASSWORD  --repo "$REPO" --body "$FDROID_PWD"
gh secret set FDROID_KEY_ALIAS          --repo "$REPO" --body "repo_signing_key"

echo "[5/6] saving local recovery copies to /mnt/HC_Volume_103952790/keystores-backup/"
BACKUP=/mnt/HC_Volume_103952790/keystores-backup
mkdir -p "$BACKUP"
chmod 700 "$BACKUP"
cp release.jks fdroid.p12 "$BACKUP/"
cat > "$BACKUP/passwords.txt" <<EOF
# Generated $(date -Iseconds). Same passwords used for keystore and key password.
# Recovery: re-upload these to GH secrets if Actions ever loses them.
RELEASE_KEYSTORE_PASSWORD=$KEYSTORE_PWD
RELEASE_KEY_ALIAS=upload
RELEASE_KEY_PASSWORD=$KEYSTORE_PWD
RELEASE_KEY_SHA256=$RELEASE_KEY_FP

FDROID_KEYSTORE_PASSWORD=$FDROID_PWD
FDROID_KEY_ALIAS=repo_signing_key
FDROID_KEY_SHA256=$FDROID_KEY_FP
EOF
chmod 600 "$BACKUP"/*

echo "[6/6] done. summary:"
echo
echo "  Backups:  $BACKUP/"
echo "  Secrets:  gh secret list --repo $REPO"
echo
echo "  APK signing cert SHA256:    $RELEASE_KEY_FP"
echo "  F-Droid index cert SHA256:  $FDROID_KEY_FP"
echo
echo "  Add this fingerprint to F-Droid Add Repository UI for verified install:"
echo "    URL:         https://d33mobile.github.io/notification-http/fdroid/repo"
echo "    Fingerprint: ${FDROID_KEY_FP//:/}"
