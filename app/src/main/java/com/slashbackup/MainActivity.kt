package com.slashbackup

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Window
import com.slashbackup.ui.screens.SlashBackupScreen
import com.slashbackup.ui.theme.ObsidianTheme
import com.slashbackup.ui.viewmodel.BackupViewModel

class MainActivity : Activity() {

    private lateinit var viewModel: BackupViewModel
    private lateinit var screen: SlashBackupScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ObsidianTheme.BgPrimary
            window.navigationBarColor = ObsidianTheme.BgCard
        }

        val app = application as SlashBackupApp
        viewModel = BackupViewModel(app.repository)
        screen = SlashBackupScreen(this, viewModel)

        setContentView(screen.getView())
        viewModel.initialize()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::screen.isInitialized && screen.currentTab != 0) {
            screen.navigateToTab(0)
        } else {
            super.onBackPressed()
        }
    }
}
