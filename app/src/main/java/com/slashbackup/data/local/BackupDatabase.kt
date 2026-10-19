package com.slashbackup.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.slashbackup.domain.model.BackupSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupSnapshotDao {
    @Query("SELECT * FROM backup_snapshots ORDER BY createdAt DESC")
    fun observeAllSnapshots(): Flow<List<BackupSnapshotEntity>>

    @Query("SELECT * FROM backup_snapshots WHERE packageName = :packageName ORDER BY createdAt DESC")
    fun observeSnapshotsForPackage(packageName: String): Flow<List<BackupSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: BackupSnapshotEntity): Long

    @Query("DELETE FROM backup_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshotById(snapshotId: Long)

    @Query("DELETE FROM backup_snapshots")
    suspend fun clearAllSnapshots()
}

@Database(
    entities = [BackupSnapshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SlashBackupDatabase : RoomDatabase() {
    abstract fun snapshotDao(): BackupSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: SlashBackupDatabase? = null

        fun getInstance(context: Context): SlashBackupDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SlashBackupDatabase::class.java,
                    "slash_backup.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
