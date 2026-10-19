package com.slashbackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slashbackup.ui.screens.SlashBackupScreen
import com.slashbackup.ui.theme.SlashBackupTheme
import com.slashbackup.ui.viewmodel.BackupViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SlashBackupApp
        setContent {
            SlashBackupTheme {
                val backupViewModel: BackupViewModel = viewModel(
                    factory = BackupViewModel.Factory(app.repository)
                )
                SlashBackupScreen(viewModel = backupViewModel)
            }
        }
    }
}
