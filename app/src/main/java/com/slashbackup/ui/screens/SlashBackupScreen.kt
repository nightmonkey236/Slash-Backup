package com.slashbackup.ui.screens

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.slashbackup.domain.model.BackupSnapshot
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.StepStatus
import com.slashbackup.ui.theme.ObsidianTheme
import com.slashbackup.ui.viewmodel.BackupViewModel

class SlashBackupScreen(
    private val context: Context,
    private val viewModel: BackupViewModel
) {

    var currentTab: Int = 0
        private set

    private val rootContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(ObsidianTheme.BgPrimary)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private val contentScroll = ScrollView(context).apply {
        isFillViewport = true
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    }

    private val pageContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val pad = ObsidianTheme.dp(context, 16)
        setPadding(pad, pad, pad, pad)
    }

    private val bottomBar = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(ObsidianTheme.BgCard)
        val padV = ObsidianTheme.dp(context, 8)
        setPadding(padV, padV, padV, padV)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    init {
        contentScroll.addView(pageContainer)
        rootContainer.addView(buildTopHeader())
        rootContainer.addView(contentScroll)
        rootContainer.addView(bottomBar)

        viewModel.setStateListener {
            renderActiveTab()
        }
        renderBottomNav()
        renderActiveTab()
    }

    fun getView(): View = rootContainer

    fun navigateToTab(index: Int) {
        currentTab = index
        renderBottomNav()
        renderActiveTab()
    }

    private fun buildTopHeader(): View {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ObsidianTheme.BgCard)
            val padH = ObsidianTheme.dp(context, 16)
            val padV = ObsidianTheme.dp(context, 14)
            setPadding(padH, padV, padH, padV)
        }

        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(context).apply {
            text = "Slash Backup"
            setTextColor(ObsidianTheme.TextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val apiBadge = TextView(context).apply {
            text = "API 24–35 • ROOT"
            setTextColor(ObsidianTheme.EmeraldPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            val pH = ObsidianTheme.dp(context, 10)
            val pV = ObsidianTheme.dp(context, 4)
            setPadding(pH, pV, pH, pV)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.EmeraldDark,
                ObsidianTheme.BorderEmerald,
                1,
                8
            )
        }

        titleRow.addView(titleText)
        titleRow.addView(apiBadge)

        val subtitle = TextView(context).apply {
            text = "Custom ROM App + Data Migration • TAR/ZSTD • SELinux Context Repair"
            setTextColor(ObsidianTheme.TextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, ObsidianTheme.dp(context, 4), 0, 0)
        }

        header.addView(titleRow)
        header.addView(subtitle)
        return header
    }

    private fun renderBottomNav() {
        bottomBar.removeAllViews()
        val tabs = listOf("Apps & Data", "ROM Migrate", "Snapshots", "Root Shell")
        for (i in tabs.indices) {
            val active = currentTab == i
            val btn = TextView(context).apply {
                text = tabs[i]
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    if (active) Typeface.BOLD else Typeface.NORMAL
                )
                setTextColor(if (active) ObsidianTheme.EmeraldPrimary else ObsidianTheme.TextSecondary)
                val padV = ObsidianTheme.dp(context, 10)
                setPadding(0, padV, 0, padV)
                background = if (active) {
                    ObsidianTheme.roundedBox(
                        context,
                        ObsidianTheme.EmeraldDark,
                        ObsidianTheme.BorderEmerald,
                        1,
                        10
                    )
                } else {
                    null
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val m = ObsidianTheme.dp(context, 3)
                    setMargins(m, 0, m, 0)
                }
                setOnClickListener {
                    navigateToTab(i)
                }
            }
            bottomBar.addView(btn)
        }
    }

    private fun renderActiveTab() {
        pageContainer.removeAllViews()
        pageContainer.addView(buildRootTelemetryBanner())
        pageContainer.addView(buildStatusBanner())

        when (currentTab) {
            0 -> renderAppsBackupTab()
            1 -> renderRomMigrationTab()
            2 -> renderSnapshotsTab()
            3 -> renderRootTerminalTab()
        }
    }

    private fun buildRootTelemetryBanner(): View {
        val env = viewModel.rootState
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgCard,
                ObsidianTheme.BorderSubtle,
                1,
                12
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 10)
            }
        }

        val line1 = TextView(context).apply {
            val stateLabel = if (env.isRootGranted) "UID 0 GRANTED" else "SANDBOX / READY"
            text = "SU Engine: ${env.providerName} ($stateLabel)"
            setTextColor(ObsidianTheme.EmeraldPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val freeGb = env.storageFreeBytes / (1024 * 1024 * 1024).coerceAtLeast(1)
        val line2 = TextView(context).apply {
            text = "SELinux: ${env.selinuxStatus} • Compression: ${viewModel.compression.name} • Free: ${freeGb}GB"
            setTextColor(ObsidianTheme.TextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, ObsidianTheme.dp(context, 4), 0, 0)
        }

        card.addView(line1)
        card.addView(line2)
        return card
    }

    private fun buildStatusBanner(): View {
        return TextView(context).apply {
            text = viewModel.statusBanner
            setTextColor(ObsidianTheme.CyanAccent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            val pad = ObsidianTheme.dp(context, 10)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.CyanDark,
                ObsidianTheme.CyanAccent,
                1,
                10
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 12)
            }
        }
    }

    private fun renderAppsBackupTab() {
        val controlsCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgCard,
                ObsidianTheme.BorderSubtle,
                1,
                12
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 12)
            }
        }

        val searchInput = EditText(context).apply {
            hint = "Search installed packages (e.g. magisk, termux)..."
            setHintTextColor(ObsidianTheme.TextMuted)
            setTextColor(ObsidianTheme.TextPrimary)
            setText(viewModel.searchQuery)
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            val pad = ObsidianTheme.dp(context, 10)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgPrimary,
                ObsidianTheme.BorderSubtle,
                1,
                8
            )
        }

        val filterRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ObsidianTheme.dp(context, 8), 0, ObsidianTheme.dp(context, 8))
        }

        filterRow.addView(
            makeChipButton(
                label = if (viewModel.showSystemApps) "Showing: User + System" else "Showing: User Apps",
                active = viewModel.showSystemApps
            ) {
                viewModel.toggleShowSystemApps()
            }
        )
        filterRow.addView(
            makeChipButton(label = "Select All", active = false) {
                viewModel.selectAllVisible(true)
            }
        )
        filterRow.addView(
            makeChipButton(label = "Clear", active = false) {
                viewModel.selectAllVisible(false)
            }
        )

        val optionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, ObsidianTheme.dp(context, 8))
        }
        optionsRow.addView(
            makeChipButton(
                label = if (viewModel.includeApk) "[✓] Base APK" else "[ ] Base APK",
                active = viewModel.includeApk
            ) {
                viewModel.toggleIncludeApk()
            }
        )
        optionsRow.addView(
            makeChipButton(
                label = if (viewModel.includeData) "[✓] /data/data" else "[ ] /data/data",
                active = viewModel.includeData
            ) {
                viewModel.toggleIncludeData()
            }
        )
        optionsRow.addView(
            makeChipButton(
                label = "Codec: ${viewModel.compression.name}",
                active = true
            ) {
                val values = CompressionType.values()
                val next = values[(viewModel.compression.ordinal + 1) % values.size]
                viewModel.setCompressionMode(next)
            }
        )

        val selectedCount = viewModel.allApps.count { it.selected }
        val backupBtn = Button(context).apply {
            text = if (viewModel.isBusy) {
                "Archiving with Root TAR+ZSTD..."
            } else {
                "Run Root Backup ($selectedCount Selected)"
            }
            isAllCaps = false
            setTextColor(ObsidianTheme.BgPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.EmeraldPrimary,
                ObsidianTheme.BorderEmerald,
                1,
                10
            )
            setOnClickListener {
                viewModel.SearchQuerySync(searchInput.text?.toString() ?: "")
                viewModel.runBatchBackup()
            }
        }

        val searchBtnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        searchInput.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val filterApplyBtn = makeChipButton("Filter", true) {
            viewModel.updateSearchQuery(searchInput.text?.toString() ?: "")
        }
        searchBtnRow.addView(searchInput)
        searchBtnRow.addView(filterApplyBtn)

        controlsCard.addView(searchBtnRow)
        controlsCard.addView(filterRow)
        controlsCard.addView(optionsRow)
        controlsCard.addView(backupBtn)
        pageContainer.addView(controlsCard)

        val apps = viewModel.filteredApps().take(40)
        val countHeader = TextView(context).apply {
            text = "INSTALLED DEVICE PACKAGES (${apps.size})"
            setTextColor(ObsidianTheme.TextMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 0, 0, ObsidianTheme.dp(context, 8))
        }
        pageContainer.addView(countHeader)

        for (app in apps) {
            pageContainer.addView(buildAppItemCard(app))
        }
    }

    private fun BackupViewModel.SearchQuerySync(q: String) {
        if (q != searchQuery) {
            updateSearchQuery(q)
        }
    }

    private fun buildAppItemCard(app: InstalledAppItem): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                if (app.selected) ObsidianTheme.EmeraldDark else ObsidianTheme.BgCard,
                if (app.selected) ObsidianTheme.EmeraldPrimary else ObsidianTheme.BorderSubtle,
                1,
                10
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 8)
            }
            setOnClickListener {
                viewModel.toggleAppSelection(app.packageName)
            }
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(context).apply {
            val checkPrefix = if (app.selected) "[✓] " else "[ ] "
            text = "$checkPrefix${app.appName} (v${app.versionName})"
            setTextColor(ObsidianTheme.TextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val uidBadge = TextView(context).apply {
            text = "UID ${app.uid}"
            setTextColor(if (app.isSystemApp) ObsidianTheme.AmberWarning else ObsidianTheme.EmeraldPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
        }

        topRow.addView(title)
        topRow.addView(uidBadge)

        val pkgText = TextView(context).apply {
            val apkMb = (app.apkSizeBytes / (1024 * 1024)).coerceAtLeast(1)
            val dataMb = (app.dataSizeBytes / (1024 * 1024)).coerceAtLeast(1)
            text = "${app.packageName} • APK: ${apkMb}MB • Data: ${dataMb}MB"
            setTextColor(ObsidianTheme.TextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setPadding(0, ObsidianTheme.dp(context, 4), 0, 0)
        }

        card.addView(topRow)
        card.addView(pkgText)
        return card
    }

    private fun renderRomMigrationTab() {
        val introCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 14)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgCard,
                ObsidianTheme.BorderEmerald,
                1,
                12
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 12)
            }
        }

        val heading = TextView(context).apply {
            text = "Custom ROM Post-Flash Migration Engine"
            setTextColor(ObsidianTheme.TextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val desc = TextView(context).apply {
            text = "When switching between LineageOS, PixelOS, Evolution X, or AOSP, Android assigns new Linux UIDs to restored apps. This 6-step root pipeline automatically detects the new UID via stat -c '%u' and repairs SELinux labels."
            setTextColor(ObsidianTheme.TextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, ObsidianTheme.dp(context, 6), 0, ObsidianTheme.dp(context, 12))
        }

        val runMigrationBtn = Button(context).apply {
            text = if (viewModel.isBusy) "Running 6-Step Migration..." else "Execute 6-Step ROM Migration Restore"
            isAllCaps = false
            setTextColor(ObsidianTheme.BgPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.EmeraldPrimary,
                ObsidianTheme.BorderEmerald,
                1,
                10
            )
            setOnClickListener {
                viewModel.runRomMigrationPipeline()
            }
        }

        introCard.addView(heading)
        introCard.addView(desc)
        introCard.addView(runMigrationBtn)
        pageContainer.addView(introCard)

        for (step in viewModel.migrationSteps) {
            val stepCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val pad = ObsidianTheme.dp(context, 12)
                setPadding(pad, pad, pad, pad)
                val border = when (step.status) {
                    StepStatus.COMPLETED -> ObsidianTheme.EmeraldPrimary
                    StepStatus.RUNNING -> ObsidianTheme.CyanAccent
                    else -> ObsidianTheme.BorderSubtle
                }
                background = ObsidianTheme.roundedBox(context, ObsidianTheme.BgCard, border, 1, 10)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = ObsidianTheme.dp(context, 8)
                }
            }

            val stepTitle = TextView(context).apply {
                val badge = when (step.status) {
                    StepStatus.COMPLETED -> "[DONE]"
                    StepStatus.RUNNING -> "[RUNNING]"
                    StepStatus.FAILED -> "[FAILED]"
                    StepStatus.READY -> "[STEP ${step.id}]"
                }
                text = "$badge ${step.title}"
                setTextColor(
                    if (step.status == StepStatus.COMPLETED) ObsidianTheme.EmeraldPrimary
                    else ObsidianTheme.TextPrimary
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val stepCmd = TextView(context).apply {
                text = "$ ${step.command}"
                setTextColor(ObsidianTheme.CyanAccent)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.MONOSPACE
                setPadding(0, ObsidianTheme.dp(context, 4), 0, ObsidianTheme.dp(context, 4))
            }

            val stepDesc = TextView(context).apply {
                text = step.description
                setTextColor(ObsidianTheme.TextSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }

            stepCard.addView(stepTitle)
            stepCard.addView(stepCmd)
            stepCard.addView(stepDesc)
            pageContainer.addView(stepCard)
        }
    }

    private fun renderSnapshotsTab() {
        val header = TextView(context).apply {
            text = "SAVED SQLITE BACKUP SNAPSHOTS (${viewModel.snapshots.size})"
            setTextColor(ObsidianTheme.TextMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 0, 0, ObsidianTheme.dp(context, 8))
        }
        pageContainer.addView(header)

        for (snap in viewModel.snapshots) {
            pageContainer.addView(buildSnapshotCard(snap))
        }
    }

    private fun buildSnapshotCard(snap: BackupSnapshot): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgCard,
                ObsidianTheme.BorderSubtle,
                1,
                10
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 10)
            }
        }

        val title = TextView(context).apply {
            val sizeMb = (snap.sizeBytes / (1024 * 1024)).coerceAtLeast(1)
            text = "${snap.appName} v${snap.versionName} • ${sizeMb}MB (${snap.compression})"
            setTextColor(ObsidianTheme.TextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val pathView = TextView(context).apply {
            text = "${snap.archivePath}\nSELinux: ${snap.selinuxContext}"
            setTextColor(ObsidianTheme.TextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setPadding(0, ObsidianTheme.dp(context, 4), 0, ObsidianTheme.dp(context, 8))
        }

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow.addView(
            makeChipButton("Restore + Fix UID/SELinux", true) {
                viewModel.runSnapshotRestore(snap)
            }
        )
        actionRow.addView(
            makeChipButton("Delete Archive", false) {
                viewModel.deleteSnapshot(snap)
            }
        )

        card.addView(title)
        card.addView(pathView)
        card.addView(actionRow)
        return card
    }

    private fun renderRootTerminalTab() {
        val cmdCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgCard,
                ObsidianTheme.BorderSubtle,
                1,
                12
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ObsidianTheme.dp(context, 10)
            }
        }

        val input = EditText(context).apply {
            hint = "root@android:# Enter su command (e.g. id, getenforce, df -h)..."
            setHintTextColor(ObsidianTheme.TextMuted)
            setTextColor(ObsidianTheme.EmeraldPrimary)
            setSingleLine(true)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val pad = ObsidianTheme.dp(context, 10)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgTerminal,
                ObsidianTheme.BorderSubtle,
                1,
                8
            )
        }

        val quickScroll = HorizontalScrollView(context).apply {
            setPadding(0, ObsidianTheme.dp(context, 8), 0, ObsidianTheme.dp(context, 8))
        }
        val quickRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val quickCmds = listOf(
            "id",
            "getenforce",
            "df -h /data",
            "pm list packages -3",
            "ls -la /sdcard/SlashBackup"
        )
        for (q in quickCmds) {
            quickRow.addView(
                makeChipButton(q, false) {
                    viewModel.executeTerminalCommand(q)
                }
            )
        }
        quickScroll.addView(quickRow)

        val execBtn = Button(context).apply {
            text = "Execute in Root Shell (su -c)"
            isAllCaps = false
            setTextColor(ObsidianTheme.BgPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.EmeraldPrimary,
                ObsidianTheme.BorderEmerald,
                1,
                10
            )
            setOnClickListener {
                val cmdText = input.text?.toString() ?: ""
                input.setText("")
                viewModel.executeTerminalCommand(cmdText)
            }
        }

        cmdCard.addView(input)
        cmdCard.addView(quickScroll)
        cmdCard.addView(execBtn)
        pageContainer.addView(cmdCard)

        val consoleBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = ObsidianTheme.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            background = ObsidianTheme.roundedBox(
                context,
                ObsidianTheme.BgTerminal,
                ObsidianTheme.BorderSubtle,
                1,
                10
            )
        }

        val logsText = TextView(context).apply {
            text = viewModel.terminalLogs.joinToString("\n")
            setTextColor(ObsidianTheme.EmeraldPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
        }
        consoleBox.addView(logsText)
        pageContainer.addView(consoleBox)
    }

    private fun makeChipButton(label: String, active: Boolean, onClick: () -> Unit): View {
        return TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(if (active) ObsidianTheme.EmeraldPrimary else ObsidianTheme.TextSecondary)
            val pH = ObsidianTheme.dp(context, 10)
            val pV = ObsidianTheme.dp(context, 6)
            setPadding(pH, pV, pH, pV)
            background = ObsidianTheme.roundedBox(
                context,
                if (active) ObsidianTheme.EmeraldDark else ObsidianTheme.BgElevated,
                if (active) ObsidianTheme.BorderEmerald else ObsidianTheme.BorderSubtle,
                1,
                8
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = ObsidianTheme.dp(context, 6)
            }
            setOnClickListener { onClick() }
        }
    }
}
