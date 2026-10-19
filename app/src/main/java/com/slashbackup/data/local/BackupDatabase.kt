package com.slashbackup.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.slashbackup.domain.model.BackupSnapshot

class SlashBackupDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
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
            """.trimIndent()
        )
        seedDefaultSnapshotsIfEmpty(db)
    }

    private fun seedDefaultSnapshotsIfEmpty(db: SQLiteDatabase) {
        val defaults = listOf(
            BackupSnapshot(
                id = "snap-magisk-01",
                packageName = "com.topjohnwu.magisk",
                appName = "Magisk",
                versionName = "28.1",
                versionCode = 28100L,
                archivePath = "/sdcard/SlashBackup/com.topjohnwu.magisk/20261019_120000",
                compression = "ZSTD",
                sizeBytes = 19_450_000L,
                includeApk = true,
                includeData = true,
                selinuxContext = "u:object_r:app_data_file:s0:c512,c768",
                createdAt = System.currentTimeMillis() - 86_400_000L
            ),
            BackupSnapshot(
                id = "snap-termux-02",
                packageName = "com.termux",
                appName = "Termux",
                versionName = "0.119.1",
                versionCode = 119L,
                archivePath = "/sdcard/SlashBackup/com.termux/20261019_133000",
                compression = "ZSTD",
                sizeBytes = 148_200_000L,
                includeApk = true,
                includeData = true,
                selinuxContext = "u:object_r:app_data_file:s0:c512,c768",
                createdAt = System.currentTimeMillis() - 36_000_000L
            )
        )
        for (item in defaults) {
            insertSnapshotInternal(db, item)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS backup_snapshots")
        onCreate(db)
    }

    private fun insertSnapshotInternal(db: SQLiteDatabase, snapshot: BackupSnapshot) {
        val values = ContentValues().apply {
            put("id", snapshot.id)
            put("packageName", snapshot.packageName)
            put("appName", snapshot.appName)
            put("versionName", snapshot.versionName)
            put("versionCode", snapshot.versionCode)
            put("archivePath", snapshot.archivePath)
            put("compression", snapshot.compression)
            put("sizeBytes", snapshot.sizeBytes)
            put("includeApk", if (snapshot.includeApk) 1 else 0)
            put("includeData", if (snapshot.includeData) 1 else 0)
            put("selinuxContext", snapshot.selinuxContext)
            put("createdAt", snapshot.createdAt)
        }
        db.insertWithOnConflict("backup_snapshots", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun upsertSnapshot(snapshot: BackupSnapshot) {
        insertSnapshotInternal(writableDatabase, snapshot)
    }

    fun deleteSnapshot(id: String) {
        writableDatabase.delete("backup_snapshots", "id = ?", arrayOf(id))
    }

    fun getAllSnapshots(): List<BackupSnapshot> {
        val list = mutableListOf<BackupSnapshot>()
        val cursor = readableDatabase.query(
            "backup_snapshots",
            null,
            null,
            null,
            null,
            null,
            "createdAt DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    BackupSnapshot(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        packageName = c.getString(c.getColumnIndexOrThrow("packageName")),
                        appName = c.getString(c.getColumnIndexOrThrow("appName")),
                        versionName = c.getString(c.getColumnIndexOrThrow("versionName")),
                        versionCode = c.getLong(c.getColumnIndexOrThrow("versionCode")),
                        archivePath = c.getString(c.getColumnIndexOrThrow("archivePath")),
                        compression = c.getString(c.getColumnIndexOrThrow("compression")),
                        sizeBytes = c.getLong(c.getColumnIndexOrThrow("sizeBytes")),
                        includeApk = c.getInt(c.getColumnIndexOrThrow("includeApk")) == 1,
                        includeData = c.getInt(c.getColumnIndexOrThrow("includeData")) == 1,
                        selinuxContext = c.getString(c.getColumnIndexOrThrow("selinuxContext")),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "slash_backup.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: SlashBackupDatabase? = null

        fun getInstance(context: Context): SlashBackupDatabase {
            return instance ?: synchronized(this) {
                instance ?: SlashBackupDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}
