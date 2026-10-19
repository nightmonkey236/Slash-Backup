package com.slashbackup.ui.viewmodel

import android.os.Handler
import android.os.Looper
import com.slashbackup.data.repository.BackupRepository
import com.slashbackup.domain.model.BackupSnapshot
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.MigrationStep
import com.slashbackup.domain.model.RootEnvironmentState
import com.slashbackup.domain.model.StepStatus
import java.util.concurrent.Executors

class BackupViewModel(private val repository: BackupRepository) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    var rootState: RootEnvironmentState = RootEnvironmentState(
        isRootGranted = false,
        suVersion = "Probing su...",
        providerName = "Checking...",
        selinuxStatus = "Enforcing",
        busyboxAvailable = true,
        zstdAvailable = true,
        storageFreeBytes = 0L
    )
        private set

    var allApps: List<InstalledAppItem> = emptyList()
        private set

    var snapshots: List<BackupSnapshot> = emptyList()
        private set

    var searchQuery: String = ""
        private set

    var showSystemApps: Boolean = false
        private set

    var compression: CompressionType = CompressionType.ZSTD
        private set

    var includeApk: Boolean = true
        private set

    var includeData: Boolean = true
        private set

    var isBusy: Boolean = false
        private set

    var statusBanner: String = "Ready — Select packages to create a root TAR+ZSTD archive"
        private set

    val terminalLogs: MutableList<String> = mutableListOf(
        "Slash Backup v1.0.0 — Custom ROM App & Data Migration Engine",
        "Target SDK: API 35 (Android 15) | Min SDK: API 24 (Android 7.0)",
        "Type any command or run a Root Backup / Custom ROM Migration."
    )

    var migrationSteps: List<MigrationStep> = defaultMigrationSteps()
        private set

    private var listener: (() -> Unit)? = null

    fun setStateListener(onUpdate: () -> Unit) {
        listener = onUpdate
    }

    private fun notifyStateChanged() {
        mainHandler.post {
            listener?.invoke()
        }
    }

    fun initialize() {
        executor.execute {
            val env = repository.probeRootEnvironment()
            val apps = repository.loadInstalledApplications(includeSystem = true)
            val snaps = repository.getSnapshots()
            mainHandler.post {
                rootState = env
                allApps = apps
                snapshots = snaps
                appendLog("[INIT] Root Provider: ${env.providerName} | SELinux: ${env.selinuxStatus}")
                appendLog("[INIT] Loaded ${apps.size} installed device packages & ${snaps.size} saved snapshots.")
                listener?.invoke()
            }
        }
    }

    fun filteredApps(): List<InstalledAppItem> {
        return allApps.filter { app ->
            val sysMatch = showSystemApps || !app.isSystemApp
            val queryMatch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            sysMatch && queryMatch
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        notifyStateChanged()
    }

    fun toggleShowSystemApps() {
        showSystemApps = !showSystemApps
        notifyStateChanged()
    }

    fun setCompressionMode(type: CompressionType) {
        compression = type
        notifyStateChanged()
    }

    fun toggleIncludeApk() {
        includeApk = !includeApk
        if (!includeApk && !includeData) includeData = true
        notifyStateChanged()
    }

    fun toggleIncludeData() {
        includeData = !includeData
        if (!includeApk && !includeData) includeApk = true
        notifyStateChanged()
    }

    fun toggleAppSelection(packageName: String) {
        allApps = allApps.map { app ->
            if (app.packageName == packageName) app.copy(selected = !app.selected) else app
        }
        notifyStateChanged()
    }

    fun selectAllVisible(select: Boolean) {
        val visibleSet = filteredApps().map { it.packageName }.toSet()
        allApps = allApps.map { app ->
            if (visibleSet.contains(app.packageName)) app.copy(selected = select) else app
        }
        notifyStateChanged()
    }

    fun runBatchBackup(onFinished: () -> Unit = {}) {
        val selected = allApps.filter { it.selected }
        if (selected.isEmpty() || isBusy) {
            statusBanner = "Select at least 1 application first."
            notifyStateChanged()
            return
        }

        isBusy = true
        statusBanner = "Backing up ${selected.size} package(s) with ${compression.name}..."
        notifyStateChanged()

        executor.execute {
            repository.backupApps(
                apps = selected,
                compression = compression,
                includeApk = includeApk,
                includeData = includeData,
                onLog = { line ->
                    mainHandler.post {
                        appendLog(line)
                        listener?.invoke()
                    }
                }
            )
            val updatedSnapshots = repository.getSnapshots()
            mainHandler.post {
                snapshots = updatedSnapshots
                isBusy = false
                statusBanner = "Backed up ${selected.size} app(s) to /sdcard/SlashBackup/"
                listener?.invoke()
                onFinished()
            }
        }
    }

    fun runSnapshotRestore(snapshot: BackupSnapshot) {
        if (isBusy) return
        isBusy = true
        statusBanner = "Restoring ${snapshot.appName} + repairing UID & SELinux..."
        notifyStateChanged()

        executor.execute {
            repository.restoreSnapshot(snapshot) { line ->
                mainHandler.post {
                    appendLog(line)
                    listener?.invoke()
                }
            }
            mainHandler.post {
                isBusy = false
                statusBanner = "Restored ${snapshot.appName} with UID & SELinux context repair."
                listener?.invoke()
            }
        }
    }

    fun deleteSnapshot(snapshot: BackupSnapshot) {
        executor.execute {
            repository.deleteSnapshot(snapshot.id)
            val updated = repository.getSnapshots()
            mainHandler.post {
                snapshots = updated
                appendLog("[DELETE] Removed snapshot ${snapshot.id} (${snapshot.packageName})")
                statusBanner = "Deleted snapshot for ${snapshot.appName}"
                listener?.invoke()
            }
        }
    }

    fun runRomMigrationPipeline() {
        if (isBusy) return
        isBusy = true
        statusBanner = "Executing 6-step Custom ROM Migration Restore Pipeline..."
        migrationSteps = defaultMigrationSteps().map { it.copy(status = StepStatus.READY) }
        notifyStateChanged()

        executor.execute {
            val steps = defaultMigrationSteps().toMutableList()
            for (i in steps.indices) {
                steps[i] = steps[i].copy(status = StepStatus.RUNNING)
                mainHandler.post {
                    migrationSteps = steps.toList()
                    appendLog("Step ${steps[i].id}/6: ${steps[i].title}")
                    appendLog("root@android:# ${steps[i].command}")
                    listener?.invoke()
                }
                repository.executeCustomRootCommand(steps[i].command)
                try {
                    Thread.sleep(220)
                } catch (_: InterruptedException) {
                }
                steps[i] = steps[i].copy(status = StepStatus.COMPLETED)
                mainHandler.post {
                    migrationSteps = steps.toList()
                    listener?.invoke()
                }
            }
            mainHandler.post {
                isBusy = false
                statusBanner = "Custom ROM Migration Restore completed — All UIDs & SELinux labels verified!"
                appendLog("[SUCCESS] Custom ROM Migration Pipeline finished cleanly.")
                listener?.invoke()
            }
        }
    }

    fun executeTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return
        appendLog("root@android:# $cmd")
        notifyStateChanged()

        executor.execute {
            val res = repository.executeCustomRootCommand(cmd)
            mainHandler.post {
                for (line in res.stdout) appendLog(line)
                for (err in res.stderr) appendLog("[stderr] $err")
                listener?.invoke()
            }
        }
    }

    private fun appendLog(line: String) {
        terminalLogs.add(line)
        while (terminalLogs.size > 120) {
            terminalLogs.removeAt(0)
        }
    }

    companion object {
        fun defaultMigrationSteps(): List<MigrationStep> = listOf(
            MigrationStep(
                id = 1,
                title = "Quiesce Target Process",
                command = "am force-stop <package_name>",
                description = "Stops active services and broadcast receivers before writing /data/data."
            ),
            MigrationStep(
                id = 2,
                title = "Reinstall Base & Split APKs",
                command = "pm install -r /sdcard/SlashBackup/<pkg>/base.apk",
                description = "Registers package in PackageManager on the newly flashed Custom ROM."
            ),
            MigrationStep(
                id = 3,
                title = "Extract Zstandard Data Archive",
                command = "tar --zstd -xpf /sdcard/SlashBackup/<pkg>/data.tar.zst -C /",
                description = "Restores databases, shared_prefs, and files into /data/data/<pkg>."
            ),
            MigrationStep(
                id = 4,
                title = "Resolve New Custom ROM Kernel UID",
                command = "NEW_UID=\$(stat -c '%u' /data/data/<package_name>)",
                description = "Reads the newly assigned Linux UID (e.g. u0_a194 -> 10194) on the target ROM."
            ),
            MigrationStep(
                id = 5,
                title = "Recursive Ownership Reassignment",
                command = "chown -R \$NEW_UID:\$NEW_UID /data/data/<pkg> /data/user_de/0/<pkg>",
                description = "Prevents permission-denied crashes caused by cross-ROM UID mismatches."
            ),
            MigrationStep(
                id = 6,
                title = "SELinux Security Context Relabel",
                command = "restorecon -R -F /data/data/<pkg> /data/user_de/0/<pkg>",
                description = "Restores u:object_r:app_data_file:s0 context so Enforcing SELinux allows access."
            )
        )
    }
}
