package com.slashbackup.domain.model

enum class CompressionType(val flag: String, val extension: String, val label: String) {
    ZSTD("--zstd", "tar.zst", "Zstandard (zstd -3)"),
    GZIP("-z", "tar.gz", "Gzip (deflate)"),
    LZ4("--use-compress-program=lz4", "tar.lz4", "LZ4 Fast"),
    NONE("", "tar", "Uncompressed TAR")
}

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val uid: Int,
    val isSystemApp: Boolean,
    val sourceDir: String,
    val dataDir: String,
    val apkSizeBytes: Long,
    val dataSizeBytes: Long,
    val selected: Boolean = false
)

data class BackupSnapshot(
    val id: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val archivePath: String,
    val compression: String,
    val sizeBytes: Long,
    val includeApk: Boolean,
    val includeData: Boolean,
    val selinuxContext: String,
    val createdAt: Long
)

data class RootEnvironmentState(
    val isRootGranted: Boolean,
    val suVersion: String,
    val providerName: String,
    val selinuxStatus: String,
    val busyboxAvailable: Boolean,
    val zstdAvailable: Boolean,
    val storageFreeBytes: Long
)

enum class StepStatus {
    READY,
    RUNNING,
    COMPLETED,
    FAILED
}

data class MigrationStep(
    val id: Int,
    val title: String,
    val command: String,
    val description: String,
    val status: StepStatus = StepStatus.READY
)
