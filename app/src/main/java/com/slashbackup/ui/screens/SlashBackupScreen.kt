package com.slashbackup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slashbackup.domain.model.BackupOperationState
import com.slashbackup.domain.model.BackupSnapshotEntity
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.ui.viewmodel.BackupViewModel
import com.slashbackup.ui.viewmodel.SlashBackupUiState

@Composable
fun SlashBackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Archive, contentDescription = "Apps & Data") },
                    label = { Text("Apps & Data") },
                    modifier = Modifier.testTag("nav_tab_apps")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.SystemUpdateAlt, contentDescription = "ROM Migration") },
                    label = { Text("Snapshots (${snapshots.size})") },
                    modifier = Modifier.testTag("nav_tab_snapshots")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Root Shell") },
                    label = { Text("Root Shell") },
                    modifier = Modifier.testTag("nav_tab_shell")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            RootStatusBanner(
                uiState = uiState,
                onRefresh = viewModel::refreshEnvironmentAndApps
            )

            OperationProgressCard(
                operationState = uiState.operationState,
                onDismiss = viewModel::dismissOperationBanner
            )

            when (selectedTab) {
                0 -> AppsBackupTab(
                    uiState = uiState,
                    onSearchChange = viewModel::onSearchQueryChange,
                    onToggleSystem = viewModel::onToggleSystemApps,
                    onSelectCompression = viewModel::onSelectCompression,
                    onToggleSelect = viewModel::onTogglePackageSelection,
                    onSelectAll = viewModel::selectAllVisible,
                    onBackupSelected = viewModel::backupSelectedPackages
                )
                1 -> SnapshotsRestoreTab(
                    snapshots = snapshots,
                    onRestore = viewModel::restoreSnapshot,
                    onDelete = viewModel::deleteSnapshot
                )
                2 -> RootShellTerminalTab(
                    uiState = uiState,
                    onExecuteCommand = viewModel::executeCustomShellCommand
                )
            }
        }
    }
}

@Composable
private fun RootStatusBanner(
    uiState: SlashBackupUiState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("root_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Root Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Slash Backup • ${uiState.rootState.rootProvider}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Slot ${uiState.rootState.activeSlot} • SELinux ${uiState.rootState.selinuxMode} • ${uiState.rootState.romBuildId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_root_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Root Status")
            }
        }
    }
}

@Composable
private fun OperationProgressCard(
    operationState: BackupOperationState,
    onDismiss: () -> Unit
) {
    when (operationState) {
        is BackupOperationState.Idle -> Unit
        is BackupOperationState.Running -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Processing ${operationState.currentPackage} (${operationState.completedCount + 1}/${operationState.totalCount})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = operationState.currentStep,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { operationState.progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is BackupOperationState.Completed -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Completed")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = operationState.summaryMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }
        is BackupOperationState.Error -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Error (${operationState.packageName}): ${operationState.reason}",
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsBackupTab(
    uiState: SlashBackupUiState,
    onSearchChange: (String) -> Unit,
    onToggleSystem: (Boolean) -> Unit,
    onSelectCompression: (CompressionType) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onBackupSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Filter package or app name...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_apps_input"),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.includeSystemApps,
                onClick = { onToggleSystem(!uiState.includeSystemApps) },
                label = { Text("System Apps") },
                modifier = Modifier.testTag("filter_system_apps_chip")
            )
            CompressionType.entries.forEach { comp ->
                FilterChip(
                    selected = uiState.compression == comp,
                    onClick = { onSelectCompression(comp) },
                    label = { Text(comp.name) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.selectedApps.size} selected / ${uiState.filteredApps.size} packages",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSelectAll(uiState.selectedApps.isEmpty()) },
                    modifier = Modifier.testTag("toggle_select_all_button")
                ) {
                    Text(if (uiState.selectedApps.isEmpty()) "Select All" else "Clear")
                }
                Button(
                    onClick = onBackupSelected,
                    enabled = uiState.selectedApps.isNotEmpty(),
                    modifier = Modifier.testTag("backup_selected_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Backup")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup (${uiState.selectedApps.size})")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.filteredApps, key = { it.packageName }) { app ->
                AppPackageCard(
                    app = app,
                    onToggle = { onToggleSelect(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun AppPackageCard(
    app: InstalledAppItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .testTag("app_item_${app.packageName}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = app.isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${app.packageName} • v${app.versionName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "APK: ${formatBytes(app.apkSizeBytes)} • /data/data: ${formatBytes(app.dataSizeBytes)} • ${if (app.hasSplitApks) "Split APKs" else "Base APK"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SnapshotsRestoreTab(
    snapshots: List<BackupSnapshotEntity>,
    onRestore: (BackupSnapshotEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (snapshots.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No root backup snapshots yet. Select apps in the first tab to create a Zstandard tar archive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(snapshots, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("snapshot_card_${item.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${item.appName} (v${item.versionName})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = item.archivePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.compression} • ${formatBytes(item.sizeBytes)} • SHA256: ${item.sha256Checksum}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { onRestore(item) },
                            modifier = Modifier.testTag("restore_snapshot_${item.id}")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore Snapshot")
                        }
                        IconButton(
                            onClick = { onDelete(item.id) },
                            modifier = Modifier.testTag("delete_snapshot_${item.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Snapshot")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootShellTerminalTab(
    uiState: SlashBackupUiState,
    onExecuteCommand: (String) -> Unit
) {
    var commandInput by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                label = { Text("su -c command (e.g. restorecon -Rv /data/data)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("root_command_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    onExecuteCommand(commandInput)
                    commandInput = ""
                },
                modifier = Modifier.testTag("send_root_command_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run Root Command")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.shellLogs, key = { it.id }) { log ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "root@android:/ # ${log.command}",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = log.output,
                        fontFamily = FontFamily.Monospace,
                        color = if (log.isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}
