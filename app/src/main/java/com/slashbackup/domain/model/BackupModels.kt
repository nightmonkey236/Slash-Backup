package com.slashbackup.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class CompressionType(val flag: String, val extension: String, val label: String) {
    ZSTD("-I 'zstd -3 -T0'", "tar.zst", "Zstandard (Fastest)"),
    GZIP("-z", "tar.gz", "Gzip (Balanced)"),
    NONE("", "tar", "Uncompressed Tar")
}

@Serializable
data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val apkSizeBytes: Long,
    val dataSizeBytes: Long,
    val externalMediaBytes: Long,
    val hasSplitApks: Boolean,
    val selinuxContext: String,
    val lastBackedUpTimestamp: Long? = null,
    val isSelected: Boolean = false
) {
    val totalSizeBytes: Long
        get() = apkSizeBytes + dataSizeBytes + externalMediaBytes
}

@Entity(tableName = "backup_snapshots")
@Serializable
data class BackupSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val archivePath: String,
    val compression: String,
    val includeApk: Boolean,
    val includeInternalData: Boolean,
    val includeExternalMedia: Boolean,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val romFingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class RootEnvironmentState(
    val isRootGranted: Boolean = false,
    val rootProvider: String = "Checking...",
    val suVersion: String = "Unknown",
    val selinuxMode: String = "Enforcing",
    val activeSlot: String = "_a",
    val romBuildId: String = "Android 15 (AP4A.250205.002)",
    val busyboxAvailable: Boolean = true,
    val zstdAvailable: Boolean = true,
    val freeStorageBytes: Long = 68_400_000_000L
)

@Serializable
data class ShellLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val output: String,
    val exitCode: Int,
    val isSuccess: Boolean = exitCode == 0
)

sealed interface BackupOperationState {
    data object Idle : BackupOperationState
    data class Running(
        val currentPackage: String,
        val currentStep: String,
        val completedCount: Int,
        val totalCount: Int,
        val progressFraction: Float
    ) : BackupOperationState
    data class Completed(
        val processedPackages: Int,
        val totalBytesProcessed: Long,
        val elapsedMs: Long,
        val summaryMessage: String
    ) : BackupOperationState
    data class Error(
        val packageName: String,
        val reason: String
    ) : BackupOperationState
}
