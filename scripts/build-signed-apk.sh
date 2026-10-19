#!/usr/bin/env bash
set -euo pipefail

KEYSTORE_FILE="${KEYSTORE_FILE:-release-key.jks}"
KEY_ALIAS="${KEY_ALIAS:-slashbackup}"
STORE_PASS="${KEYSTORE_PASSWORD:-slashbackup123}"
KEY_PASS="${KEY_PASSWORD:-slashbackup123}"

echo "========================================================"
echo "  Slash Backup — Signed Release APK Builder"
echo "========================================================"

if [ ! -f "$KEYSTORE_FILE" ]; then
  echo "[1/3] Generating RSA-2048 V2/V3 Release Keystore: $KEYSTORE_FILE"
  keytool -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" \
    -dname "CN=Slash Backup, OU=RootTools, O=SlashBackup, L=Colombo, S=Western, C=LK"
else
  echo "[1/3] Using existing Release Keystore: $KEYSTORE_FILE"
fi

cat > keystore.properties <<EOF
storeFile=$KEYSTORE_FILE
storePassword=$STORE_PASS
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASS
EOF

echo "[2/3] Running Gradle assembleRelease with V1/V2/V3/V4 Signing..."
if [ -x "./gradlew" ]; then
  ./gradlew assembleRelease
else
  gradle assembleRelease
fi

APK_PATH="app/build/outputs/apk/release/app-release.apk"
echo "[3/3] Signed APK generated at: $APK_PATH"
