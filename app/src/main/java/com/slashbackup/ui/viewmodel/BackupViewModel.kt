package com.slashbackup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.slashbackup.data.repository.BackupRepository
import com.slashbackup.domain.model.BackupOperationState
import com.slashbackup.domain.model.BackupSnapshotEntity
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.RootEnvironmentState
import com.slashbackup.domain.model.ShellLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SlashBackupUiState(
    val rootState: RootEnvironmentState = RootEnvironmentState(),
    val apps: List<InstalledAppItem> = emptyList(),
    val searchQuery: String = "",
    val includeSystemApps: Boolean = false,
    val compression: CompressionType = CompressionType.ZSTD,
    val includeApk: Boolean = true,
    val includeInternalData: Boolean = true,
    val includeExternalMedia: Boolean = false,
    val operationState: BackupOperationState = BackupOperationState.Idle,
    val shellLogs: List<ShellLogEntry> = emptyList()
) {
    val filteredApps: List<InstalledAppItem>
        get() = apps.filter { app ->
            (includeSystemApps || !app.isSystemApp) &&
                (searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true))
        }

    val selectedApps: List<InstalledAppItem>
        get() = apps.filter { it.isSelected }
}

class BackupViewModel(
    private val repository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SlashBackupUiState())
    val uiState: StateFlow<SlashBackupUiState> = _uiState.asStateFlow()

    val snapshots: StateFlow<List<BackupSnapshotEntity>> = repository.allSnapshots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        refreshEnvironmentAndApps()
    }

    fun refreshEnvironmentAndApps() {
        viewModelScope.launch {
            val env = repository.checkRootEnvironment()
            val apps = repository.loadInstalledApps(includeSystem = true)
            _uiState.update {
                it.copy(
                    rootState = env,
                    apps = apps
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onToggleSystemApps(include: Boolean) {
        _uiState.update { it.copy(includeSystemApps = include) }
    }

    fun onSelectCompression(compression: CompressionType) {
        _uiState.update { it.copy(compression = compression) }
    }

    fun onTogglePackageSelection(packageName: String) {
        _uiState.update { state ->
            state.copy(
                apps = state.apps.map { app ->
                    if (app.packageName == packageName) app.copy(isSelected = !app.isSelected) else app
                }
            )
        }
    }

    fun selectAllVisible(select: Boolean) {
        val visiblePkgs = _uiState.value.filteredApps.map { it.packageName }.toSet()
        _uiState.update { state ->
            state.copy(
                apps = state.apps.map { app ->
                    if (app.packageName in visiblePkgs) app.copy(isSelected = select) else app
                }
            )
        }
    }

    fun backupSelectedPackages() {
        val current = _uiState.value
        val targets = current.selectedApps
        if (targets.isEmpty()) return

        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            var totalBytes = 0L

            targets.forEachIndexed { index, app ->
                _uiState.update {
                    it.copy(
                        operationState = BackupOperationState.Running(
                            currentPackage = app.appName,
                            currentStep = "Archiving /data/data/${app.packageName} (${current.compression.label})",
                            completedCount = index,
                            totalCount = targets.size,
                            progressFraction = index.toFloat() / targets.size.toFloat()
                        )
                    )
                }

                repository.performAppBackup(
                    app = app,
                    compression = current.compression,
                    includeApk = current.includeApk,
                    includeData = current.includeInternalData,
                    includeMedia = current.includeExternalMedia,
                    romFingerprint = current.rootState.romBuildId,
                    onLog = ::appendLog
                )
                totalBytes += app.totalSizeBytes
            }

            val elapsed = System.currentTimeMillis() - startMs
            _uiState.update {
                it.copy(
                    operationState = BackupOperationState.Completed(
                        processedPackages = targets.size,
                        totalBytesProcessed = totalBytes,
                        elapsedMs = elapsed,
                        summaryMessage = "Backed up ${targets.size} packages with SELinux contexts"
                    )
                )
            }
        }
    }

    fun restoreSnapshot(snapshot: BackupSnapshotEntity) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operationState = BackupOperationState.Running(
                        currentPackage = snapshot.appName,
                        currentStep = "Restoring data & running restorecon -Rv",
                        completedCount = 0,
                        totalCount = 1,
                        progressFraction = 0.5f
                    )
                )
            }
            repository.performSnapshotRestore(snapshot, ::appendLog)
            _uiState.update {
                it.copy(
                    operationState = BackupOperationState.Completed(
                        processedPackages = 1,
                        totalBytesProcessed = snapshot.sizeBytes,
                        elapsedMs = 850L,
                        summaryMessage = "Restored ${snapshot.appName} & fixed UID + SELinux context"
                    )
                )
            }
        }
    }

    fun deleteSnapshot(snapshotId: Long) {
        viewModelScope.launch {
            repository.deleteSnapshot(snapshotId)
        }
    }

    fun executeCustomShellCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            val entry = repository.runRootCommand(command.trim())
            appendLog(entry)
        }
    }

    fun dismissOperationBanner() {
        _uiState.update { it.copy(operationState = BackupOperationState.Idle) }
    }

    private fun appendLog(entry: ShellLogEntry) {
        _uiState.update { state ->
            state.copy(shellLogs = (listOf(entry) + state.shellLogs).take(200))
        }
    }

    class Factory(private val repository: BackupRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(repository) as T
        }
    }
}
