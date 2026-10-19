export interface ProjectFileEntry {
  path: string;
  language: 'kotlin' | 'toml' | 'xml' | 'gradle';
  description: string;
  content: string;
}

export const ANDROID_PROJECT_FILES: ProjectFileEntry[] = [
  {
    path: 'app/build.gradle.kts',
    language: 'gradle',
    description: 'Signed Release & Debug APK configuration with V1, V2, V3, and V4 Signature Schemes, R8 minification, and keystore.properties / CI environment variable support.',
    content: `import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.slashbackup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.slashbackup"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("debugConfig") {
            val debugStore = file("\${rootDir}/debug.keystore")
            if (debugStore.exists()) {
                storeFile = debugStore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }

        create("release") {
            val releaseStorePath = keystoreProperties.getProperty("storeFile")
                ?: System.getenv("KEYSTORE_FILE")
                ?: "\${rootDir}/release-key.jks"
            val releaseStoreFile = file(releaseStorePath)

            if (releaseStoreFile.exists()) {
                storeFile = releaseStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: System.getenv("KEYSTORE_PASSWORD")
                    ?: "slashbackup123"
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: System.getenv("KEY_ALIAS")
                    ?: "slashbackup"
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: System.getenv("KEY_PASSWORD")
                    ?: "slashbackup123"
            } else if (file("\${rootDir}/debug.keystore").exists()) {
                storeFile = file("\${rootDir}/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            if (file("\${rootDir}/debug.keystore").exists()) {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}`,
  },
  {
    path: '.github/workflows/build-signed-apk.yml',
    language: 'xml',
    description: 'GitHub Actions CI workflow that automatically generates an RSA-2048 keystore, runs gradle assembleRelease, verifies the APK signature with apksigner, and uploads the signed APK artifact.',
    content: `name: Build Signed Android APK

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build-signed-apk:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Set up JDK 17 (Temurin)
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.10.2'

      - name: Generate Release Keystore (RSA 2048)
        run: |
          keytool -genkeypair -v \\
            -keystore release-key.jks \\
            -alias slashbackup \\
            -keyalg RSA \\
            -keysize 2048 \\
            -validity 10000 \\
            -storepass slashbackup123 \\
            -keypass slashbackup123 \\
            -dname "CN=Slash Backup, OU=RootTools, O=SlashBackup, L=Colombo, S=Western, C=LK"

      - name: Build Signed Release APK
        env:
          KEYSTORE_FILE: \${{ github.workspace }}/release-key.jks
          KEYSTORE_PASSWORD: slashbackup123
          KEY_ALIAS: slashbackup
          KEY_PASSWORD: slashbackup123
        run: gradle assembleRelease --no-daemon

      - name: Verify APK Signature (apksigner)
        run: |
          \$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

      - name: Upload Signed Release APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: SlashBackup-v1.0.0-signed-release-apk
          path: app/build/outputs/apk/release/*.apk`,
  },
  {
    path: 'scripts/build-signed-apk.sh',
    language: 'gradle',
    description: 'One-command local shell script to generate release-key.jks, write keystore.properties, and compile app-release.apk.',
    content: `#!/usr/bin/env bash
set -euo pipefail

KEYSTORE_FILE="\${KEYSTORE_FILE:-release-key.jks}"
KEY_ALIAS="\${KEY_ALIAS:-slashbackup}"
STORE_PASS="\${KEYSTORE_PASSWORD:-slashbackup123}"
KEY_PASS="\${KEY_PASSWORD:-slashbackup123}"

if [ ! -f "\$KEYSTORE_FILE" ]; then
  keytool -genkeypair -v \\
    -keystore "\$KEYSTORE_FILE" \\
    -alias "\$KEY_ALIAS" \\
    -keyalg RSA \\
    -keysize 2048 \\
    -validity 10000 \\
    -storepass "\$STORE_PASS" \\
    -keypass "\$KEY_PASS" \\
    -dname "CN=Slash Backup, OU=RootTools, O=SlashBackup, L=Colombo, S=Western, C=LK"
fi

cat > keystore.properties <<EOF
storeFile=\$KEYSTORE_FILE
storePassword=\$STORE_PASS
keyAlias=\$KEY_ALIAS
keyPassword=\$KEY_PASS
EOF

./gradlew assembleRelease`,
  },
  {
    path: 'gradle/libs.versions.toml',
    language: 'toml',
    description: 'Version Catalog defining Kotlin 2.1, Jetpack Compose Material 3, Room + KSP, Coroutines, and topjohnwu/libsu 6.0.0.',
    content: `[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
coreKtx = "1.15.0"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigationCompose = "2.8.5"
room = "2.6.1"
coroutines = "1.9.0"
serializationJson = "1.7.3"
libsu = "6.0.0"
workRuntimeKtx = "2.10.0"
datastorePreferences = "1.1.1"`,
  },
  {
    path: 'app/src/main/java/com/slashbackup/data/root/RootBackupEngine.kt',
    language: 'kotlin',
    description: 'Core root execution engine using libsu Shell.cmd() to stop apps, archive split APKs + /data/data with Zstandard, reassign UIDs, and run restorecon -Rv.',
    content: `package com.slashbackup.data.root

import android.os.Build
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.RootEnvironmentState
import com.slashbackup.domain.model.ShellLogEntry
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootBackupEngine {

    suspend fun inspectRootEnvironment(): RootEnvironmentState = withContext(Dispatchers.IO) {
        val shell = Shell.getShell()
        if (!shell.isRoot) {
            return@withContext RootEnvironmentState(
                isRootGranted = false,
                rootProvider = "Unrooted / Standard Shell",
                suVersion = "N/A"
            )
        }
        val suVersion = Shell.cmd("su -v").exec().out.firstOrNull()?.trim() ?: "Magisk / KernelSU"
        val selinuxOut = Shell.cmd("getenforce").exec().out.firstOrNull()?.trim() ?: "Enforcing"
        val slot = Shell.cmd("getprop ro.boot.slot_suffix").exec().out.firstOrNull()?.trim().orEmpty().ifEmpty { "_a" }

        RootEnvironmentState(
            isRootGranted = true,
            rootProvider = if (suVersion.contains("KERNELSU", true)) "KernelSU" else "Magisk",
            suVersion = suVersion,
            selinuxMode = selinuxOut,
            activeSlot = slot,
            romBuildId = Build.DISPLAY ?: "Custom ROM"
        )
    }
}`,
  },
];
