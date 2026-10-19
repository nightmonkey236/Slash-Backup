package com.slashbackup.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.slashbackup.data.local.SlashBackupDatabase
import com.slashbackup.data.root.RootBackupEngine
import com.slashbackup.domain.model.BackupSnapshot
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.RootEnvironmentState
import java.io.File
import java.util.UUID

class BackupRepository(
    private val context: Context,
    private val database: SlashBackupDatabase,
    private val rootEngine: RootBackupEngine
) {

    fun probeRootEnvironment(): RootEnvironmentState {
        return rootEngine.probeRootEnvironment()
    }

    fun loadInstalledApplications(includeSystem: Boolean): List<InstalledAppItem> {
        val pm = context.packageManager
        val packages = try {
            pm.getInstalledPackages(0)
        } catch (_: Throwable) {
            emptyList()
        }

        val items = mutableListOf<InstalledAppItem>()
        for (pkgInfo in packages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) continue

            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Throwable) {
                pkgInfo.packageName
            }

            val versionName = pkgInfo.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            val sourceFile = File(appInfo.sourceDir ?: "")
            val apkBytes = if (sourceFile.exists()) sourceFile.length() else 18_500_000L
            val estimatedDataBytes = (apkBytes * 6L) / 5L + 4_200_000L

            items.add(
                InstalledAppItem(
                    packageName = pkgInfo.packageName,
                    appName = label,
                    versionName = versionName,
                    versionCode = versionCode,
                    uid = appInfo.uid,
                    isSystemApp = isSystem,
                    sourceDir = appInfo.sourceDir ?: "/data/app/${pkgInfo.packageName}/base.apk",
                    dataDir = appInfo.dataDir ?: "/data/data/${pkgInfo.packageName}",
                    apkSizeBytes = apkBytes,
                    dataSizeBytes = estimatedDataBytes,
                    selected = false
                )
            )
        }

        return items.sortedWith(
            compareBy<InstalledAppItem> { it.isSystemApp }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun getSnapshots(): List<BackupSnapshot> {
        return database.getAllSnapshots()
    }

    fun backupApps(
        apps: List<InstalledAppItem>,
        compression: CompressionType,
        includeApk: Boolean,
        includeData: Boolean,
        onLog: (String) -> Unit
    ): List<BackupSnapshot> {
        val created = mutableListOf<BackupSnapshot>()
        for (app in apps) {
            val destPath = rootEngine.backupApplication(
                app = app,
                compression = compression,
                includeApk = includeApk,
                includeData = includeData,
                onLog = onLog
            )
            val totalSize = (if (includeApk) app.apkSizeBytes else 0L) +
                (if (includeData) app.dataSizeBytes else 0L)
            val snapshot = BackupSnapshot(
                id = "snap-" + UUID.randomUUID().toString().take(8),
                packageName = app.packageName,
                appName = app.appName,
                versionName = app.versionName,
                versionCode = app.versionCode,
                archivePath = destPath,
                compression = compression.name,
                sizeBytes = totalSize,
                includeApk = includeApk,
                includeData = includeData,
                selinuxContext = "u:object_r:app_data_file:s0:c512,c768",
                createdAt = System.currentTimeMillis()
            )
            database.upsertSnapshot(snapshot)
            created.add(snapshot)
        }
        return created
    }

    fun restoreSnapshot(snapshot: BackupSnapshot, onLog: (String) -> Unit): Boolean {
        return rootEngine.restoreApplicationWithMigrationFix(
            packageName = snapshot.packageName,
            appName = snapshot.appName,
            archivePath = snapshot.archivePath,
            onLog = onLog
        )
    }

    fun deleteSnapshot(id: String) {
        database.deleteSnapshot(id)
    }

    fun executeCustomRootCommand(command: String): RootBackupEngine.ShellExecutionResult {
        return rootEngine.executeRootCommand(command)
    }
}
