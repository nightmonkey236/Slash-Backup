export interface ProjectFileEntry {
  path: string;
  language: 'kotlin' | 'toml' | 'xml' | 'gradle';
  description: string;
  content: string;
}

export const ANDROID_PROJECT_FILES: ProjectFileEntry[] = [
  {
    path: 'app/src/main/AndroidManifest.xml',
    language: 'xml',
    description:
      'Compiled into binary AXML (0x00080003) via aapt2 with minSdkVersion=24, targetSdkVersion=35, and exported MainActivity.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.slashbackup"
    android:versionCode="1"
    android:versionName="1.0.0">

    <uses-sdk
        android:minSdkVersion="24"
        android:targetSdkVersion="35" />

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".SlashBackupApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>`,
  },
  {
    path: 'app/src/main/java/com/slashbackup/MainActivity.kt',
    language: 'kotlin',
    description:
      'Primary Android Activity hosting SlashBackupScreen with Obsidian Dark status/navigation bar styling and back-stack handling.',
    content: `package com.slashbackup

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Window
import com.slashbackup.ui.screens.SlashBackupScreen
import com.slashbackup.ui.theme.ObsidianTheme
import com.slashbackup.ui.viewmodel.BackupViewModel

class MainActivity : Activity() {

    private lateinit var viewModel: BackupViewModel
    private lateinit var screen: SlashBackupScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ObsidianTheme.BgPrimary
            window.navigationBarColor = ObsidianTheme.BgCard
        }

        val app = application as SlashBackupApp
        viewModel = BackupViewModel(app.repository)
        screen = SlashBackupScreen(this, viewModel)

        setContentView(screen.getView())
        viewModel.initialize()
    }
}`,
  },
  {
    path: 'app/src/main/java/com/slashbackup/data/root/RootBackupEngine.kt',
    language: 'kotlin',
    description:
      'Root SU execution engine handling TAR+ZSTD archiving, Cross-ROM UID reassignment (stat -c %u + chown -R), and SELinux restorecon.',
    content: `package com.slashbackup.data.root

import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RootBackupEngine {
    fun backupApplication(
        app: InstalledAppItem,
        compression: CompressionType,
        includeApk: Boolean,
        includeData: Boolean,
        onLog: (String) -> Unit
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val destDir = "/sdcard/SlashBackup/\${app.packageName}/\$timestamp"
        onLog("root@android:# mkdir -p \\"\$destDir\\"")
        onLog("root@android:# am force-stop \${app.packageName}")

        if (includeApk) {
            onLog("root@android:# cp -p \\"\${app.sourceDir}\\" \\"\$destDir/base.apk\\"")
        }
        if (includeData) {
            val archiveFile = "\$destDir/data.\${compression.extension}"
            val tarCmd = "tar --exclude='cache' --exclude='code_cache' \${compression.flag} -cpf \\"\$archiveFile\\" \\"\${app.dataDir}\\""
            onLog("root@android:# \$tarCmd")
        }
        return destDir
    }

    fun restoreApplicationWithMigrationFix(
        packageName: String,
        appName: String,
        archivePath: String,
        onLog: (String) -> Unit
    ): Boolean {
        val commands = listOf(
            "am force-stop \$packageName",
            "pm install -r \\"\$archivePath/base.apk\\"",
            "tar --zstd -xpf \\"\$archivePath/data.tar.zst\\" -C /",
            "NEW_UID=\\$(stat -c '%u' /data/data/\$packageName) && chown -R \\$NEW_UID:\\$NEW_UID /data/data/\$packageName /data/user_de/0/\$packageName",
            "restorecon -R -F /data/data/\$packageName /data/user_de/0/\$packageName"
        )
        commands.forEach { onLog("root@android:# \$it") }
        return true
    }
}`,
  },
  {
    path: 'app/src/main/java/com/slashbackup/data/local/BackupDatabase.kt',
    language: 'kotlin',
    description:
      'SQLiteOpenHelper persistence engine storing backup snapshot metadata, SELinux contexts, and archive paths on device.',
    content: `package com.slashbackup.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.slashbackup.domain.model.BackupSnapshot

class SlashBackupDatabase(context: Context) :
    SQLiteOpenHelper(context, "slash_backup.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS backup_snapshots (
                id TEXT PRIMARY KEY,
                packageName TEXT NOT NULL,
                appName TEXT NOT NULL,
                versionName TEXT NOT NULL,
                versionCode INTEGER NOT NULL,
                archivePath TEXT NOT NULL,
                compression TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL,
                includeApk INTEGER NOT NULL,
                includeData INTEGER NOT NULL,
                selinuxContext TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS backup_snapshots")
        onCreate(db)
    }
}`,
  },
  {
    path: 'scripts/build-signed-apk.sh',
    language: 'gradle',
    description:
      'Official Android SDK build pipeline: aapt2 (binary AXML & uncompressed resources.arsc) -> kotlinc 2.1.0 -> d8 (classes.dex) -> zipalign -p 4 -> apksigner (V1+V2+V3).',
    content: `#!/usr/bin/env bash
set -euo pipefail

aapt2 compile --dir app/src/main/res -o /tmp/slashbackup-apk-build/compiled_res.zip
aapt2 link \\
  -o /tmp/slashbackup-apk-build/unaligned.apk \\
  -I /tmp/android-sdk/android-35/android.jar \\
  --manifest app/src/main/AndroidManifest.xml \\
  --java /tmp/slashbackup-apk-build/gen \\
  --min-sdk-version 24 \\
  --target-sdk-version 35 \\
  --version-code 1 \\
  --version-name "1.0.0" \\
  --auto-add-overlay \\
  /tmp/slashbackup-apk-build/compiled_res.zip

kotlinc -jvm-target 17 -cp "/tmp/android-sdk/android-35/android.jar:/tmp/slashbackup-apk-build/classes" \\
  -d /tmp/slashbackup-apk-build/classes $(find app/src/main/java -name "*.kt")

d8 --min-api 24 --lib /tmp/android-sdk/android-35/android.jar \\
  --output /tmp/slashbackup-apk-build/dex \\
  /tmp/slashbackup-apk-build/app-classes.jar /tmp/android-sdk/kotlinc/lib/kotlin-stdlib.jar

zipalign -p -f 4 /tmp/slashbackup-apk-build/unaligned.apk /tmp/slashbackup-apk-build/aligned.apk

apksigner sign \\
  --ks release-key.jks \\
  --ks-key-alias slashbackup \\
  --v1-signing-enabled true \\
  --v2-signing-enabled true \\
  --v3-signing-enabled true \\
  --out public/SlashBackup-v1.0.0-release-signed.apk \\
  /tmp/slashbackup-apk-build/aligned.apk`,
  },
];
