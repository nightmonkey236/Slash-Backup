#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

AAPT2="/tmp/android-sdk/android-14/aapt2"
D8="/tmp/android-sdk/android-14/d8"
ZIPALIGN="/tmp/android-sdk/android-14/zipalign"
APKSIGNER="/tmp/android-sdk/android-14/apksigner"
ANDROID_JAR="/tmp/android-sdk/android-35/android.jar"
KOTLINC="/tmp/android-sdk/kotlinc/bin/kotlinc"
KOTLIN_STDLIB="/tmp/android-sdk/kotlinc/lib/kotlin-stdlib.jar"

BUILD_WORK="/tmp/slashbackup-apk-build"
rm -rf "$BUILD_WORK"
mkdir -p "$BUILD_WORK/gen" "$BUILD_WORK/classes" "$BUILD_WORK/dex"
mkdir -p "$ROOT_DIR/public" "$ROOT_DIR/.build-outputs"
mkdir -p "$ROOT_DIR/app/build/outputs/apk/debug" "$ROOT_DIR/app/build/outputs/apk/release"

KEYSTORE_FILE="$ROOT_DIR/release-key.jks"
if [ ! -f "$KEYSTORE_FILE" ]; then
  keytool -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -alias slashbackup \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass slashbackup123 \
    -keypass slashbackup123 \
    -dname "CN=Slash Backup, OU=Root Engineering, O=SlashBackup, L=Colombo, S=Western, C=LK"
fi

DEBUG_KS="$ROOT_DIR/debug.keystore"
if [ ! -f "$DEBUG_KS" ]; then
  keytool -genkeypair -v \
    -keystore "$DEBUG_KS" \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass android \
    -keypass android \
    -dname "CN=Android Debug, O=Android, C=US"
fi

echo "[1/6] Compiling Android resources with aapt2..."
"$AAPT2" compile --dir "$ROOT_DIR/app/src/main/res" -o "$BUILD_WORK/compiled_res.zip"

echo "[2/6] Linking binary AndroidManifest.xml and uncompressed resources.arsc (API 35)..."
"$AAPT2" link \
  -o "$BUILD_WORK/unaligned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT_DIR/app/src/main/AndroidManifest.xml" \
  --java "$BUILD_WORK/gen" \
  --min-sdk-version 24 \
  --target-sdk-version 35 \
  --version-code 1 \
  --version-name "1.0.0" \
  --auto-add-overlay \
  "$BUILD_WORK/compiled_res.zip"

echo "[3/6] Compiling generated R.java and Kotlin sources (Kotlin 2.1.0, JVM 17)..."
javac --release 17 \
  -cp "$ANDROID_JAR" \
  -d "$BUILD_WORK/classes" \
  $(find "$BUILD_WORK/gen" -name "*.java")

"$KOTLINC" -jvm-target 17 \
  -cp "$ANDROID_JAR:$BUILD_WORK/classes" \
  -d "$BUILD_WORK/classes" \
  $(find "$ROOT_DIR/app/src/main/java" -name "*.kt")

echo "[4/6] Dexing compiled classes + kotlin-stdlib with d8 (minSdk 24)..."
python3 -c '
import os, zipfile
classes_dir = "/tmp/slashbackup-apk-build/classes"
jar_path = "/tmp/slashbackup-apk-build/app-classes.jar"
with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, _, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                full = os.path.join(root, f)
                rel = os.path.relpath(full, classes_dir)
                zf.write(full, rel)
'

"$D8" --min-api 24 \
  --lib "$ANDROID_JAR" \
  --output "$BUILD_WORK/dex" \
  "$BUILD_WORK/app-classes.jar" \
  "$KOTLIN_STDLIB"

python3 -c '
import zipfile, os
apk_path = "/tmp/slashbackup-apk-build/unaligned.apk"
dex_dir = "/tmp/slashbackup-apk-build/dex"
with zipfile.ZipFile(apk_path, "a") as zf:
    for f in sorted(os.listdir(dex_dir)):
        if f.endswith(".dex"):
            zf.write(os.path.join(dex_dir, f), f, compress_type=zipfile.ZIP_DEFLATED)
'

echo "[5/6] Aligning APK on 4-byte boundaries with zipalign -p -f 4..."
"$ZIPALIGN" -p -f 4 "$BUILD_WORK/unaligned.apk" "$BUILD_WORK/aligned.apk"

echo "[6/6] Signing Release & Debug APKs with V1 + V2 + V3 APK Signature Schemes..."
"$APKSIGNER" sign \
  --ks "$KEYSTORE_FILE" \
  --ks-key-alias slashbackup \
  --ks-pass pass:slashbackup123 \
  --key-pass pass:slashbackup123 \
  --min-sdk-version 21 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$ROOT_DIR/public/SlashBackup-v1.0.0-release-signed.apk" \
  "$BUILD_WORK/aligned.apk"

"$APKSIGNER" sign \
  --ks "$DEBUG_KS" \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --min-sdk-version 21 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$ROOT_DIR/.build-outputs/app-debug.apk" \
  "$BUILD_WORK/aligned.apk"

cp -f "$ROOT_DIR/public/SlashBackup-v1.0.0-release-signed.apk" "$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
cp -f "$ROOT_DIR/.build-outputs/app-debug.apk" "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -d "$ROOT_DIR/dist" ]; then
  cp -f "$ROOT_DIR/public/SlashBackup-v1.0.0-release-signed.apk" "$ROOT_DIR/dist/SlashBackup-v1.0.0-release-signed.apk"
fi

echo "Verifying APK Signatures and Binary Manifest..."
"$APKSIGNER" verify --verbose --print-certs "$ROOT_DIR/public/SlashBackup-v1.0.0-release-signed.apk"
"$APKSIGNER" verify --verbose "$ROOT_DIR/.build-outputs/app-debug.apk"
ls -lh "$ROOT_DIR/public/SlashBackup-v1.0.0-release-signed.apk" "$ROOT_DIR/.build-outputs/app-debug.apk"
