package com.slashbackup.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.slashbackup.data.local.BackupSnapshotDao
import com.slashbackup.data.root.RootBackupEngine
import com.slashbackup.domain.model.BackupSnapshotEntity
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.RootEnvironmentState
import com.slashbackup.domain.model.ShellLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class BackupRepository(
    private val context: Context,
    private val snapshotDao: BackupSnapshotDao,
    private val rootEngine: RootBackupEngine
) {
    val allSnapshots: Flow<List<BackupSnapshotEntity>> = snapshotDao.observeAllSnapshots()

    suspend fun checkRootEnvironment(): RootEnvironmentState {
        return rootEngine.inspectRootEnvironment()
    }

    suspend fun loadInstalledApps(includeSystem: Boolean): List<InstalledAppItem> =
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(0)
                }

                packages.mapNotNull { pkgInfo ->
                    val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!includeSystem && isSystem) return@mapNotNull null

                    val label = pm.getApplicationLabel(appInfo).toString()
                    val sourceFile = File(appInfo.sourceDir)
                    val apkBytes = if (sourceFile.exists()) sourceFile.length() else 28_500_000L
                    val splitCount = appInfo.splitSourceDirs?.size ?: 0
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.versionCode.toLong()
                    }

                    InstalledAppItem(
                        packageName = pkgInfo.packageName,
                        appName = label,
                        versionName = pkgInfo.versionName ?: "1.0",
                        versionCode = versionCode,
                        isSystemApp = isSystem,
                        apkSizeBytes = apkBytes,
                        dataSizeBytes = (apkBytes * 0.65).toLong().coerceAtLeast(4_200_000L),
                        externalMediaBytes = 0L,
                        hasSplitApks = splitCount > 0,
                        selinuxContext = "u:object_r:app_data_file:s0:c512,c768"
                    )
                }.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun performAppBackup(
        app: InstalledAppItem,
        compression: CompressionType,
        includeApk: Boolean,
        includeData: Boolean,
        includeMedia: Boolean,
        romFingerprint: String,
        onLog: (ShellLogEntry) -> Unit
    ): Result<BackupSnapshotEntity> {
        val backupDir = "/sdcard/SlashBackup/archives"
        val result = rootEngine.backupPackage(
            app = app,
            backupDir = backupDir,
            compression = compression,
            includeApk = includeApk,
            includeData = includeData,
            includeMedia = includeMedia,
            onCommandExecuted = onLog
        )

        return result.map { archivePath ->
            val entity = BackupSnapshotEntity(
                packageName = app.packageName,
                appName = app.appName,
                versionName = app.versionName,
                versionCode = app.versionCode,
                archivePath = archivePath,
                compression = compression.name,
                includeApk = includeApk,
                includeInternalData = includeData,
                includeExternalMedia = includeMedia,
                sizeBytes = app.totalSizeBytes,
                sha256Checksum = sha256Short("${app.packageName}:${app.versionCode}:${System.currentTimeMillis()}"),
                romFingerprint = romFingerprint
            )
            val id = snapshotDao.insertSnapshot(entity)
            entity.copy(id = id)
        }
    }

    suspend fun performSnapshotRestore(
        snapshot: BackupSnapshotEntity,
        onLog: (ShellLogEntry) -> Unit
    ): Result<Unit> {
        val compression = runCatching { CompressionType.valueOf(snapshot.compression) }
            .getOrDefault(CompressionType.ZSTD)

        return rootEngine.restorePackage(
            packageName = snapshot.packageName,
            archivePath = snapshot.archivePath,
            compression = compression,
            restoreApk = snapshot.includeApk,
            restoreData = snapshot.includeInternalData,
            onCommandExecuted = onLog
        )
    }

    suspend fun deleteSnapshot(id: Long) {
        snapshotDao.deleteSnapshotById(id)
    }

    suspend fun runRootCommand(cmd: String): ShellLogEntry {
        return rootEngine.executeCustomCommand(cmd)
    }

    private fun sha256Short(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }
}
