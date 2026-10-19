package com.slashbackup

import android.app.Application
import com.slashbackup.data.local.SlashBackupDatabase
import com.slashbackup.data.repository.BackupRepository
import com.slashbackup.data.root.RootBackupEngine
import com.topjohnwu.superuser.Shell

class SlashBackupApp : Application() {

    lateinit var repository: BackupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
        )

        val database = SlashBackupDatabase.getInstance(this)
        val rootEngine = RootBackupEngine()
        repository = BackupRepository(
            context = this,
            snapshotDao = database.snapshotDao(),
            rootEngine = rootEngine
        )
    }
}
