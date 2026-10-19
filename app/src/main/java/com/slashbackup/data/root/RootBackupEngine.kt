package com.slashbackup.data.root

import android.os.Build
import com.slashbackup.domain.model.CompressionType
import com.slashbackup.domain.model.InstalledAppItem
import com.slashbackup.domain.model.RootEnvironmentState
import com.slashbackup.domain.model.ShellLogEntry
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes privileged root (`su`) operations via libsu for backing up and restoring
 * `/data/app`, `/data/data/<pkg>`, `/sdcard/Android/media/<pkg>`, UID ownership,
 * and SELinux file security contexts (`restorecon -Rv`) across custom ROM flashes.
 */
class RootBackupEngine {

    suspend fun inspectRootEnvironment(): RootEnvironmentState = withContext(Dispatchers.IO) {
        try {
            val shell = Shell.getShell()
            val isRoot = shell.isRoot
            if (!isRoot) {
                return@withContext RootEnvironmentState(
                    isRootGranted = false,
                    rootProvider = "Unrooted / Standard Shell",
                    suVersion = "N/A",
                    selinuxMode = "Enforcing",
                    activeSlot = readSystemSlot(),
                    romBuildId = Build.DISPLAY ?: "Android"
                )
            }

            val suVerResult = Shell.cmd("su -v").exec()
            val suVersion = suVerResult.out.firstOrNull()?.trim() ?: "Magisk / KernelSU"
            val provider = when {
                suVersion.contains("KERNELSU", ignoreCase = true) -> "KernelSU"
                suVersion.contains("APATCH", ignoreCase = true) -> "APatch"
                suVersion.contains("MAGISK", ignoreCase = true) -> "Magisk"
                else -> "Superuser (UID 0)"
            }

            val selinuxOut = Shell.cmd("getenforce").exec().out.firstOrNull()?.trim() ?: "Enforcing"
            val zstdCheck = Shell.cmd("which zstd").exec().isSuccess
            val busyboxCheck = Shell.cmd("which busybox || which toybox").exec().isSuccess

            RootEnvironmentState(
                isRootGranted = true,
                rootProvider = provider,
                suVersion = suVersion,
                selinuxMode = selinuxOut,
                activeSlot = readSystemSlot(),
                romBuildId = Build.DISPLAY ?: "Custom ROM",
                busyboxAvailable = busyboxCheck,
                zstdAvailable = zstdCheck
            )
        } catch (e: Exception) {
            RootEnvironmentState(
                isRootGranted = false,
                rootProvider = "Error: ${e.localizedMessage ?: "Root Check Failed"}",
                suVersion = "N/A"
            )
        }
    }

    private fun readSystemSlot(): String {
        val result = Shell.cmd("getprop ro.boot.slot_suffix").exec()
        val slot = result.out.firstOrNull()?.trim().orEmpty()
        return if (slot.isNotEmpty()) slot else "_a"
    }

    suspend fun backupPackage(
        app: InstalledAppItem,
        backupDir: String,
        compression: CompressionType,
        includeApk: Boolean,
        includeData: Boolean,
        includeMedia: Boolean,
        onCommandExecuted: (ShellLogEntry) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pkg = app.packageName
            val targetFolder = "$backupDir/$pkg"
            val archiveFile = "$targetFolder/${pkg}_${app.versionCode}.${compression.extension}"

            val commands = buildList {
                add("mkdir -p \"$targetFolder\"")
                add("am force-stop $pkg")
                if (includeApk) {
                    add("APK_DIR=\$(pm path $pkg | head -n 1 | cut -d: -f2 | xargs dirname) && cp -rf \"\$APK_DIR\"/*.apk \"$targetFolder/\"")
                }
                if (includeData) {
                    val tarCmd = if (compression.flag.isNotEmpty()) {
                        "tar ${compression.flag} -cpf \"$archiveFile\" --exclude='cache/*' --exclude='code_cache/*' -C /data/data \"$pkg\""
                    } else {
                        "tar -cpf \"$archiveFile\" --exclude='cache/*' --exclude='code_cache/*' -C /data/data \"$pkg\""
                    }
                    add(tarCmd)
                }
                if (includeMedia) {
                    add("[ -d \"/sdcard/Android/media/$pkg\" ] && tar -cpf \"$targetFolder/media.tar\" -C /sdcard/Android/media \"$pkg\" || true")
                }
                add("sha256sum \"$archiveFile\" | cut -d' ' -f1 > \"$targetFolder/checksum.sha256\" || true")
            }

            for (cmd in commands) {
                val res = Shell.cmd(cmd).exec()
                onCommandExecuted(
                    ShellLogEntry(
                        command = cmd,
                        output = (res.out + res.err).joinToString("\n").ifEmpty { "OK" },
                        exitCode = res.code
                    )
                )
            }

            Result.success(archiveFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restorePackage(
        packageName: String,
        archivePath: String,
        compression: CompressionType,
        restoreApk: Boolean,
        restoreData: Boolean,
        onCommandExecuted: (ShellLogEntry) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val parentFolder = archivePath.substringBeforeLast("/")
            val commands = buildList {
                add("am force-stop $packageName")
                if (restoreApk) {
                    add("pm install-create -r && pm install -r \"$parentFolder\"/*.apk || true")
                }
                if (restoreData) {
                    val extractCmd = if (compression.flag.isNotEmpty()) {
                        "tar ${compression.flag} -xpf \"$archivePath\" -C /data/data"
                    } else {
                        "tar -xpf \"$archivePath\" -C /data/data"
                    }
                    add(extractCmd)
                    // Critical for Custom ROM Migration: fix UID/GID & SELinux Contexts
                    add("APP_UID=\$(stat -c '%u' /data/data/$packageName 2>/dev/null || dumpsys package $packageName | grep userId= | head -n1 | cut -d= -f2)")
                    add("chown -R \"\$APP_UID:\$APP_UID\" \"/data/data/$packageName\"")
                    add("restorecon -Rv \"/data/data/$packageName\"")
                }
            }

            for (cmd in commands) {
                val res = Shell.cmd(cmd).exec()
                onCommandExecuted(
                    ShellLogEntry(
                        command = cmd,
                        output = (res.out + res.err).joinToString("\n").ifEmpty { "OK" },
                        exitCode = res.code
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeCustomCommand(rawCommand: String): ShellLogEntry = withContext(Dispatchers.IO) {
        try {
            val res = Shell.cmd(rawCommand).exec()
            ShellLogEntry(
                command = rawCommand,
                output = (res.out + res.err).joinToString("\n").ifEmpty { "(exit ${res.code})" },
                exitCode = res.code
            )
        } catch (e: Exception) {
            ShellLogEntry(
                command = rawCommand,
                output = e.localizedMessage ?: "Execution failed",
                exitCode = 1
            )
        }
    }
}
