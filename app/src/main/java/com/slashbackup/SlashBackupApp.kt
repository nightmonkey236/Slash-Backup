package com.slashbackup

import android.app.Application
import com.slashbackup.data.local.SlashBackupDatabase
import com.slashbackup.data.repository.BackupRepository
import com.slashbackup.data.root.RootBackupEngine

class SlashBackupApp : Application() {

    lateinit var repository: BackupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = SlashBackupDatabase.getInstance(this)
        val rootEngine = RootBackupEngine()
        repository = BackupRepository(
            context = this,
            database = database,
            rootEngine = rootEngine
        )
    }
}
