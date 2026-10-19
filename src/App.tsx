import React, { useState, useMemo } from 'react';
import {
  ShieldCheck,
  Archive,
  HardDrive,
  Terminal,
  RefreshCw,
  CheckSquare,
  Square,
  Play,
  RotateCcw,
  Trash2,
  Search,
  Cpu,
  FileCode2,
  Copy,
  Check,
  Sparkles,
  Layers,
  PackageCheck,
  AlertTriangle,
  ChevronRight,
  FolderArchive,
  Send,
  KeyRound,
  Download,
  FileCheck2,
  Lock,
  Smartphone,
  Maximize2,
  Wifi,
  BatteryCharging,
} from 'lucide-react';
import { ANDROID_PROJECT_FILES } from './kotlinSourceFiles';

type CompressionMode = 'ZSTD' | 'GZIP' | 'NONE';

interface InstalledApp {
  packageName: string;
  appName: string;
  versionName: string;
  versionCode: number;
  uid: number;
  isSystem: boolean;
  hasSplitApks: boolean;
  apkSizeMb: number;
  dataSizeMb: number;
  mediaSizeMb: number;
  selinuxContext: string;
  lastBackup?: string;
  selected: boolean;
}

interface BackupSnapshot {
  id: string;
  packageName: string;
  appName: string;
  versionName: string;
  versionCode: number;
  compression: CompressionMode;
  includeApk: boolean;
  includeData: boolean;
  includeMedia: boolean;
  sizeMb: number;
  sha256: string;
  romBuild: string;
  createdAt: string;
  archivePath: string;
}

interface ShellLog {
  id: string;
  timestamp: string;
  command: string;
  output: string;
  exitCode: number;
}

const INITIAL_APPS: InstalledApp[] = [
  {
    packageName: 'org.thoughtcrime.securesms',
    appName: 'Signal Private Messenger',
    versionName: '7.28.4',
    versionCode: 149200,
    uid: 10241,
    isSystem: false,
    hasSplitApks: true,
    apkSizeMb: 74.2,
    dataSizeMb: 412.8,
    mediaSizeMb: 185.0,
    selinuxContext: 'u:object_r:app_data_file:s0:c241,c256,c512,c768',
    lastBackup: '2 hours ago',
    selected: true,
  },
  {
    packageName: 'com.valvesoftware.android.steam.community',
    appName: 'Steam Guard & Authenticator',
    versionName: '3.9.2',
    versionCode: 10942,
    uid: 10258,
    isSystem: false,
    hasSplitApks: true,
    apkSizeMb: 58.6,
    dataSizeMb: 34.1,
    mediaSizeMb: 0,
    selinuxContext: 'u:object_r:app_data_file:s0:c2,c257,c512,c768',
    lastBackup: 'Yesterday',
    selected: true,
  },
  {
    packageName: 'com.termux',
    appName: 'Termux Terminal Environment',
    versionName: '0.119.1',
    versionCode: 119,
    uid: 10264,
    isSystem: false,
    hasSplitApks: false,
    apkSizeMb: 32.0,
    dataSizeMb: 1240.5,
    mediaSizeMb: 0,
    selinuxContext: 'u:object_r:app_data_file:s0:c8,c257,c512,c768',
    lastBackup: '3 days ago',
    selected: true,
  },
  {
    packageName: 'io.github.vvb2060.mahoshojo',
    appName: 'Momo / Native Root Detector',
    versionName: '4.4.1',
    versionCode: 441,
    uid: 10277,
    isSystem: false,
    hasSplitApks: false,
    apkSizeMb: 4.8,
    dataSizeMb: 1.9,
    mediaSizeMb: 0,
    selinuxContext: 'u:object_r:app_data_file:s0:c21,c257,c512,c768',
    selected: false,
  },
  {
    packageName: 'com.beemdevelopment.aegis',
    appName: 'Aegis Authenticator (2FA)',
    versionName: '3.2.0',
    versionCode: 76,
    uid: 10283,
    isSystem: false,
    hasSplitApks: false,
    apkSizeMb: 14.3,
    dataSizeMb: 8.4,
    mediaSizeMb: 0,
    selinuxContext: 'u:object_r:app_data_file:s0:c27,c257,c512,c768',
    lastBackup: '2 hours ago',
    selected: false,
  },
  {
    packageName: 'org.lineageos.updater',
    appName: 'LineageOS OTA Updater',
    versionName: '15.0-BP1A',
    versionCode: 35,
    uid: 10042,
    isSystem: true,
    hasSplitApks: false,
    apkSizeMb: 6.1,
    dataSizeMb: 2.3,
    mediaSizeMb: 0,
    selinuxContext: 'u:object_r:system_app_data_file:s0',
    selected: false,
  },
];

const INITIAL_SNAPSHOTS: BackupSnapshot[] = [
  {
    id: 'snap-1',
    packageName: 'org.thoughtcrime.securesms',
    appName: 'Signal Private Messenger',
    versionName: '7.28.4',
    versionCode: 149200,
    compression: 'ZSTD',
    includeApk: true,
    includeData: true,
    includeMedia: true,
    sizeMb: 672.0,
    sha256: 'e3b0c44298fc1c14',
    romBuild: 'LineageOS 22.1 (Android 15 • Slot _a)',
    createdAt: '2026-10-19 05:12',
    archivePath: '/sdcard/SlashBackup/archives/org.thoughtcrime.securesms/149200.tar.zst',
  },
  {
    id: 'snap-2',
    packageName: 'com.beemdevelopment.aegis',
    appName: 'Aegis Authenticator (2FA)',
    versionName: '3.2.0',
    versionCode: 76,
    compression: 'ZSTD',
    includeApk: true,
    includeData: true,
    includeMedia: false,
    sizeMb: 22.7,
    sha256: '9f86d081884c7d65',
    romBuild: 'LineageOS 22.1 (Android 15 • Slot _a)',
    createdAt: '2026-10-19 05:10',
    archivePath: '/sdcard/SlashBackup/archives/com.beemdevelopment.aegis/76.tar.zst',
  },
];

const INITIAL_LOGS: ShellLog[] = [
  {
    id: 'log-1',
    timestamp: '07:00:01',
    command: 'su -v && getenforce && getprop ro.boot.slot_suffix',
    output: '28.1:MAGISKSU\nEnforcing\n_a',
    exitCode: 0,
  },
  {
    id: 'log-2',
    timestamp: '07:00:03',
    command: 'zstd --version && toybox --version',
    output: '*** Zstandard CLI (64-bit) v1.5.6, by Yann Collet ***\ntoybox 0.8.11-android',
    exitCode: 0,
  },
];

export default function App() {
  // Default to showing the Android App Preview ('apps') immediately so the user sees the app right away
  const [activeTab, setActiveTab] = useState<
    'apps' | 'snapshots' | 'migration' | 'shell' | 'signed_apk' | 'kotlin'
  >('apps');
  const [viewMode, setViewMode] = useState<'phone_split' | 'full_width'>('phone_split');
  const [phoneNavTab, setPhoneNavTab] = useState<'apps' | 'snapshots' | 'migration' | 'shell'>(
    'apps'
  );

  const [apps, setApps] = useState<InstalledApp[]>(INITIAL_APPS);
  const [snapshots, setSnapshots] = useState<BackupSnapshot[]>(INITIAL_SNAPSHOTS);
  const [shellLogs, setShellLogs] = useState<ShellLog[]>(INITIAL_LOGS);

  const [searchQuery, setSearchQuery] = useState('');
  const [showSystemApps, setShowSystemApps] = useState(false);
  const [compression, setCompression] = useState<CompressionMode>('ZSTD');
  const [includeApk, setIncludeApk] = useState(true);
  const [includeData, setIncludeData] = useState(true);
  const [includeMedia, setIncludeMedia] = useState(false);

  const [rootProvider, setRootProvider] = useState<
    'Magisk 28.1' | 'KernelSU 1.0.2' | 'APatch 10920'
  >('Magisk 28.1');
  const [activeOperation, setActiveOperation] = useState<{
    title: string;
    detail: string;
    progress: number;
  } | null>(null);
  const [statusBanner, setStatusBanner] = useState<string | null>(null);

  // Signed APK Builder State
  const [keystoreFile, setKeystoreFile] = useState('release-key.jks');
  const [keyAlias, setKeyAlias] = useState('slashbackup');
  const [storePassword, setStorePassword] = useState('slashbackup123');
  const [keyPassword, setKeyPassword] = useState('slashbackup123');
  const [validityYears] = useState(27);
  const [v1Enabled, setV1Enabled] = useState(true);
  const [v2Enabled, setV2Enabled] = useState(true);
  const [v3Enabled, setV3Enabled] = useState(true);
  const [v4Enabled, setV4Enabled] = useState(true);
  const [signedBuildOutput, setSignedBuildOutput] = useState({
    apkName: 'SlashBackup-v1.0.0-release-signed.apk',
    sha256Fingerprint:
      'A4:8F:2C:19:0E:7B:44:D1:93:CA:5E:80:11:6A:B2:9D:3C:4E:71:09:FA:22:18:5B:90:C4:12:E7:8A:3D:01:F5',
    builtAt: 'Ready (V1 + V2 + V3 + V4 Signed)',
    sizeMb: '4.82 MB (R8 Minified)',
  });

  // ROM Migration Builder options
  const [migrationZipName, setMigrationZipName] = useState(
    'SlashBackup_ROM_Migration_A15_SlotA.zip'
  );
  const [preserveMagiskModules, setPreserveMagiskModules] = useState(true);
  const [autoFixSelinux, setAutoFixSelinux] = useState(true);
  const [wipeDalvikAfterRestore, setWipeDalvikAfterRestore] = useState(true);
  const [preserveWifiBluetooth, setPreserveWifiBluetooth] = useState(true);

  // Root shell input
  const [customCmd, setCustomCmd] = useState('');

  // Kotlin Source Inspector state
  const [selectedFileIndex, setSelectedFileIndex] = useState(0);
  const [copiedPath, setCopiedPath] = useState<string | null>(null);

  const filteredApps = useMemo(() => {
    return apps.filter((app) => {
      if (!showSystemApps && app.isSystem) return false;
      if (
        searchQuery.trim() !== '' &&
        !app.appName.toLowerCase().includes(searchQuery.toLowerCase()) &&
        !app.packageName.toLowerCase().includes(searchQuery.toLowerCase())
      ) {
        return false;
      }
      return true;
    });
  }, [apps, showSystemApps, searchQuery]);

  const selectedApps = useMemo(() => apps.filter((a) => a.selected), [apps]);

  const totalSelectedSizeMb = useMemo(() => {
    return selectedApps.reduce((acc, app) => {
      let sum = 0;
      if (includeApk) sum += app.apkSizeMb;
      if (includeData) sum += app.dataSizeMb;
      if (includeMedia) sum += app.mediaSizeMb;
      return acc + sum;
    }, 0);
  }, [selectedApps, includeApk, includeData, includeMedia]);

  const pushLog = (command: string, output: string, exitCode = 0) => {
    const now = new Date().toTimeString().slice(0, 8);
    setShellLogs((prev) => [
      {
        id: `log-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        timestamp: now,
        command,
        output,
        exitCode,
      },
      ...prev.slice(0, 120),
    ]);
  };

  const toggleAppSelection = (packageName: string) => {
    setApps((prev) =>
      prev.map((a) => (a.packageName === packageName ? { ...a, selected: !a.selected } : a))
    );
  };

  const toggleSelectAllVisible = () => {
    const visibleSet = new Set(filteredApps.map((a) => a.packageName));
    const allVisibleSelected = filteredApps.every((a) => a.selected);
    setApps((prev) =>
      prev.map((a) => (visibleSet.has(a.packageName) ? { ...a, selected: !allVisibleSelected } : a))
    );
  };

  const runBatchBackup = () => {
    if (selectedApps.length === 0 || activeOperation) return;

    const ext = compression === 'ZSTD' ? 'tar.zst' : compression === 'GZIP' ? 'tar.gz' : 'tar';
    const flag = compression === 'ZSTD' ? "-I 'zstd -3 -T0'" : compression === 'GZIP' ? '-z' : '';

    setActiveOperation({
      title: `Backing up ${selectedApps.length} package(s) via UID 0...`,
      detail: `Stopping processes & running tar ${flag} on /data/data`,
      progress: 45,
    });

    setTimeout(() => {
      const createdSnapshots: BackupSnapshot[] = selectedApps.map((app) => {
        const archivePath = `/sdcard/SlashBackup/archives/${app.packageName}/${app.versionCode}.${ext}`;
        const cmd = `am force-stop ${app.packageName} && tar ${flag} -cpf "${archivePath}" --exclude='cache/*' -C /data/data "${app.packageName}"`;
        pushLog(
          cmd,
          `Archived ${app.packageName} (UID ${app.uid}, SELinux ${app.selinuxContext}) -> ${archivePath}`,
          0
        );

        const sizeMb = Number(
          (
            (includeApk ? app.apkSizeMb : 0) +
            (includeData ? app.dataSizeMb : 0) +
            (includeMedia ? app.mediaSizeMb : 0)
          ).toFixed(1)
        );

        return {
          id: `snap-${Date.now()}-${app.packageName}`,
          packageName: app.packageName,
          appName: app.appName,
          versionName: app.versionName,
          versionCode: app.versionCode,
          compression,
          includeApk,
          includeData,
          includeMedia,
          sizeMb,
          sha256: Math.random().toString(16).slice(2, 18),
          romBuild: 'LineageOS 22.1 (Android 15 • Slot _a)',
          createdAt: 'Just now',
          archivePath,
        };
      });

      setSnapshots((prev) => [...createdSnapshots, ...prev]);
      setApps((prev) =>
        prev.map((a) => (a.selected ? { ...a, lastBackup: 'Just now' } : a))
      );
      setActiveOperation(null);
      setStatusBanner(
        `Backed up ${createdSnapshots.length} package(s) (${totalSelectedSizeMb.toFixed(1)} MB) with SELinux contexts.`
      );
    }, 500);
  };

  const restoreSnapshot = (snap: BackupSnapshot) => {
    if (activeOperation) return;
    setActiveOperation({
      title: `Restoring ${snap.appName}...`,
      detail: `Extracting ${snap.archivePath}, reassigning UID & running restorecon -Rv`,
      progress: 70,
    });

    setTimeout(() => {
      const cmd = `am force-stop ${snap.packageName} && tar -xpf "${snap.archivePath}" -C /data/data && chown -R $(dumpsys package ${snap.packageName} | grep userId= | head -n1 | cut -d= -f2) /data/data/${snap.packageName} && restorecon -Rv /data/data/${snap.packageName}`;
      pushLog(
        cmd,
        `Restored /data/data/${snap.packageName}\nRelabeled SELinux context u:object_r:app_data_file:s0\nExit code 0`,
        0
      );
      setActiveOperation(null);
      setStatusBanner(
        `Restored ${snap.appName} (v${snap.versionName}) and repaired UID + SELinux labels.`
      );
    }, 450);
  };

  const deleteSnapshot = (id: string) => {
    const target = snapshots.find((s) => s.id === id);
    if (target) {
      pushLog(`rm -rf "${target.archivePath}"`, 'Removed archive snapshot', 0);
    }
    setSnapshots((prev) => prev.filter((s) => s.id !== id));
  };

  const generateFlashableMigrationZip = () => {
    if (activeOperation) return;
    const outPath = `/sdcard/SlashBackup/migration/${migrationZipName}`;
    setActiveOperation({
      title: 'Building Recovery/Magisk Flashable Migration Archive...',
      detail: `Packaging ${snapshots.length} snapshots + update-binary + SELinux restore script`,
      progress: 80,
    });

    setTimeout(() => {
      pushLog(
        `sh /data/adb/slashbackup/build_update_zip.sh --out "${outPath}" --selinux-restore=${autoFixSelinux} --dalvik-wipe=${wipeDalvikAfterRestore}`,
        `Generated recovery-flashable migration package: ${outPath}\nIncluded META-INF/com/google/android/update-binary\nSHA256 verified.`,
        0
      );
      setActiveOperation(null);
      setStatusBanner(
        `Flashable ROM migration package ready at ${outPath} (${snapshots.length} apps bundled).`
      );
    }, 550);
  };

  const triggerSignedApkBuild = () => {
    if (activeOperation) return;
    setActiveOperation({
      title: 'Building Signed Release APK (assembleRelease)...',
      detail: `Signing with ${keystoreFile} (alias: ${keyAlias})`,
      progress: 80,
    });

    setTimeout(() => {
      const cmd = `./scripts/build-signed-apk.sh && apksigner verify --verbose app/build/outputs/apk/release/app-release.apk`;
      pushLog(
        cmd,
        `> Task :app:minifyReleaseWithR8\n> Task :app:packageRelease\n> Task :app:signReleaseApk\nVerified using v1/v2/v3/v4 schemes\nBUILD SUCCESSFUL`,
        0
      );
      setSignedBuildOutput({
        apkName: 'SlashBackup-v1.0.0-release-signed.apk',
        sha256Fingerprint:
          'A4:8F:2C:19:0E:7B:44:D1:93:CA:5E:80:11:6A:B2:9D:3C:4E:71:09:FA:22:18:5B:90:C4:12:E7:8A:3D:01:F5',
        builtAt: 'Signed just now (V1/V2/V3/V4)',
        sizeMb: '4.82 MB (R8 Minified & Zipaligned)',
      });
      setActiveOperation(null);
      setStatusBanner(
        'Signed Release APK verified! Click "Download Signed Android Project (.zip)" to get the complete package.'
      );
    }, 550);
  };

  const handleRunCustomCommand = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = customCmd.trim();
    if (!trimmed) return;

    let simulatedOutput = `Executed via ${rootProvider} (UID 0): OK`;
    if (trimmed.includes('id')) {
      simulatedOutput = 'uid=0(root) gid=0(root) groups=0(root) context=u:r:magisk:s0';
    } else if (trimmed.includes('restorecon')) {
      simulatedOutput =
        'SELinux: Loaded file_contexts\nRelabeled /data/data packages to u:object_r:app_data_file:s0';
    } else if (trimmed.includes('df')) {
      simulatedOutput =
        'Filesystem      Size  Used Avail Use% Mounted on\n/dev/block/dm-5 118G   49G   69G  42% /data';
    } else if (trimmed.includes('getenforce')) {
      simulatedOutput = 'Enforcing';
    }

    pushLog(trimmed, simulatedOutput, 0);
    setCustomCmd('');
  };

  const copyText = (id: string, content: string) => {
    navigator.clipboard.writeText(content);
    setCopiedPath(id);
    setTimeout(() => setCopiedPath(null), 2000);
  };

  const keytoolCommand = `keytool -genkeypair -v -keystore ${keystoreFile} -alias ${keyAlias} -keyalg RSA -keysize 2048 -validity ${
    validityYears * 365
  } -storepass ${storePassword} -keypass ${keyPassword} -dname "CN=Slash Backup, OU=RootTools, O=SlashBackup, L=Colombo, S=Western, C=LK"`;

  return (
    <div className="min-h-screen bg-[#090D16] text-slate-100 flex flex-col">
      {/* Top Bar */}
      <header className="border-b border-slate-800/80 bg-[#0D1322]/95 backdrop-blur sticky top-0 z-20">
        <div className="max-w-7xl mx-auto px-4 py-2.5 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-emerald-500/15 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-base tracking-tight text-white">Slash Backup</h1>
                <span className="text-[10px] font-mono uppercase px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                  UID 0 Granted
                </span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-sky-500/15 text-sky-300 border border-sky-500/30">
                  Signed APK Ready
                </span>
              </div>
              <p className="text-[11px] text-slate-400">
                Android Root App Data Backup &amp; Custom ROM Migration Tool
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <button
              onClick={() =>
                setViewMode((m) => (m === 'phone_split' ? 'full_width' : 'phone_split'))
              }
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border flex items-center gap-1.5 transition ${
                viewMode === 'phone_split'
                  ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300'
                  : 'bg-slate-900 border-slate-700 text-slate-300 hover:bg-slate-800'
              }`}
            >
              {viewMode === 'phone_split' ? (
                <>
                  <Smartphone className="w-3.5 h-3.5" />
                  <span>Android Phone Preview: ON</span>
                </>
              ) : (
                <>
                  <Maximize2 className="w-3.5 h-3.5" />
                  <span>Full Desktop Mode</span>
                </>
              )}
            </button>

            <a
              href="/SlashBackup-Android-Signed-Project.zip"
              download="SlashBackup-Android-Signed-Project.zip"
              className="px-3 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-xs flex items-center gap-1.5 shadow transition"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download Signed Project (.zip)</span>
            </a>
          </div>
        </div>

        {/* Navigation Tabs (for Full Width / Side Panel control) */}
        <div className="max-w-7xl mx-auto px-4 flex gap-1 overflow-x-auto border-t border-slate-800/60">
          {[
            { id: 'apps', label: 'Apps & /data/data', icon: Archive, badge: `${apps.length}` },
            {
              id: 'snapshots',
              label: 'Snapshots & Restore',
              icon: RotateCcw,
              badge: `${snapshots.length}`,
            },
            { id: 'migration', label: 'ROM Migration ZIP', icon: FolderArchive },
            { id: 'shell', label: 'Root Shell (su)', icon: Terminal, badge: `${shellLogs.length}` },
            { id: 'signed_apk', label: 'Signed APK Builder', icon: KeyRound, badge: 'V2/V3/V4' },
            { id: 'kotlin', label: 'Kotlin / Compose Code', icon: FileCode2, badge: 'Android' },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => {
                  setActiveTab(tab.id as typeof activeTab);
                  if (
                    tab.id === 'apps' ||
                    tab.id === 'snapshots' ||
                    tab.id === 'migration' ||
                    tab.id === 'shell'
                  ) {
                    setPhoneNavTab(tab.id);
                  }
                }}
                className={`flex items-center gap-2 px-3.5 py-2 text-xs font-medium border-b-2 transition whitespace-nowrap ${
                  isActive
                    ? 'border-emerald-400 text-emerald-300 bg-emerald-500/10'
                    : 'border-transparent text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{tab.label}</span>
                {tab.badge && (
                  <span
                    className={`px-1.5 py-0.2 rounded text-[10px] font-mono ${
                      isActive
                        ? 'bg-emerald-500/25 text-emerald-200'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {tab.badge}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-7xl w-full mx-auto px-4 py-5 flex-1 flex flex-col gap-4">
        {/* Status / Operation Notifications */}
        {activeOperation && (
          <div className="rounded-xl bg-emerald-950/60 border border-emerald-500/40 p-3 flex flex-col gap-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-emerald-200 flex items-center gap-2">
                <Cpu className="w-4 h-4 animate-spin text-emerald-400" />
                {activeOperation.title}
              </span>
              <span className="font-mono text-emerald-300">{activeOperation.progress}%</span>
            </div>
            <p className="text-[11px] font-mono text-emerald-300/80">{activeOperation.detail}</p>
            <div className="w-full h-1.5 bg-slate-900 rounded-full overflow-hidden">
              <div
                className="h-full bg-emerald-400 transition-all duration-300"
                style={{ width: `${activeOperation.progress}%` }}
              />
            </div>
          </div>
        )}

        {statusBanner && !activeOperation && (
          <div className="rounded-xl bg-sky-950/50 border border-sky-500/40 px-4 py-2 flex items-center justify-between gap-3 text-xs text-sky-200">
            <div className="flex items-center gap-2">
              <PackageCheck className="w-4 h-4 text-sky-400 shrink-0" />
              <span>{statusBanner}</span>
            </div>
            <button
              onClick={() => setStatusBanner(null)}
              className="text-sky-400 hover:text-sky-200 font-mono text-[11px]"
            >
              DISMISS
            </button>
          </div>
        )}

        {/* SPLIT VIEW: Interactive Android Phone Emulator Frame on the Left + Controls/Signed APK on the Right */}
        {viewMode === 'phone_split' ? (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Interactive Android 15 Phone Simulator Frame */}
            <div className="lg:col-span-5 flex flex-col items-center">
              <div className="w-full max-w-[390px] rounded-[38px] bg-[#0B0F19] border-[6px] border-slate-800 shadow-2xl overflow-hidden flex flex-col h-[720px]">
                {/* Android Status Bar */}
                <div className="px-6 pt-2.5 pb-1.5 bg-[#090D16] flex items-center justify-between text-[11px] font-mono text-slate-300 select-none">
                  <span>07:21</span>
                  <div className="w-16 h-4 rounded-full bg-black border border-slate-800/80" />
                  <div className="flex items-center gap-1.5">
                    <span className="text-[10px] text-emerald-400 font-bold">ROOT</span>
                    <Wifi className="w-3.5 h-3.5 text-slate-300" />
                    <BatteryCharging className="w-4 h-4 text-emerald-400" />
                  </div>
                </div>

                {/* Android App Top AppBar + Root Status Card */}
                <div className="px-3.5 pt-2 pb-2 bg-[#090D16] border-b border-slate-800/80">
                  <div className="rounded-2xl bg-[#161F30] border border-slate-700/60 p-3 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
                        <ShieldCheck className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-xs font-bold text-white flex items-center gap-1.5">
                          <span>Slash Backup</span>
                          <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-300">
                            {rootProvider.split(' ')[0]}
                          </span>
                        </div>
                        <div className="text-[10px] font-mono text-slate-400">
                          Slot _a • SELinux Enforcing • Android 15
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        pushLog(
                          'su -c "id"',
                          `uid=0(root) gid=0(root) provider=${rootProvider}`,
                          0
                        )
                      }
                      className="p-1.5 rounded-lg bg-slate-800/90 text-slate-300 hover:text-white"
                      title="Refresh Root"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                {/* Phone Screen Body */}
                <div className="flex-1 overflow-y-auto px-3.5 py-2.5 flex flex-col gap-2.5 bg-[#090D16]">
                  {phoneNavTab === 'apps' && (
                    <>
                      {/* Search & Filter Row */}
                      <div className="flex gap-1.5">
                        <div className="relative flex-1">
                          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                          <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search packages..."
                            className="w-full bg-[#111827] border border-slate-800 rounded-xl pl-8 pr-2.5 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                          />
                        </div>
                        <button
                          onClick={() => setShowSystemApps((v) => !v)}
                          className={`px-2.5 py-1.5 rounded-xl text-[10px] font-mono border ${
                            showSystemApps
                              ? 'bg-amber-500/20 border-amber-500/40 text-amber-300'
                              : 'bg-[#111827] border-slate-800 text-slate-400'
                          }`}
                        >
                          /system
                        </button>
                      </div>

                      {/* Compression Chips + Action Bar */}
                      <div className="flex items-center justify-between gap-1">
                        <div className="flex gap-1">
                          {(['ZSTD', 'GZIP', 'NONE'] as CompressionMode[]).map((mode) => (
                            <button
                              key={mode}
                              onClick={() => setCompression(mode)}
                              className={`px-2 py-1 rounded-lg text-[10px] font-mono border ${
                                compression === mode
                                  ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-300 font-semibold'
                                  : 'bg-[#111827] border-slate-800 text-slate-400'
                              }`}
                            >
                              {mode}
                            </button>
                          ))}
                        </div>
                        <button
                          onClick={runBatchBackup}
                          disabled={selectedApps.length === 0 || !!activeOperation}
                          className="px-3 py-1 rounded-xl bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 text-slate-950 font-bold text-[11px] flex items-center gap-1 shadow"
                        >
                          <Play className="w-3 h-3 fill-current" />
                          <span>Backup ({selectedApps.length})</span>
                        </button>
                      </div>

                      {/* App Cards inside Phone */}
                      <div className="flex flex-col gap-2">
                        {filteredApps.map((app) => (
                          <div
                            key={app.packageName}
                            onClick={() => toggleAppSelection(app.packageName)}
                            className={`p-2.5 rounded-2xl border cursor-pointer transition flex items-start gap-2.5 ${
                              app.selected
                                ? 'bg-[#132030] border-emerald-500/50'
                                : 'bg-[#111827] border-slate-800/90'
                            }`}
                          >
                            <div className="mt-0.5 text-emerald-400">
                              {app.selected ? (
                                <CheckSquare className="w-4 h-4" />
                              ) : (
                                <Square className="w-4 h-4 text-slate-600" />
                              )}
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between gap-1">
                                <span className="text-xs font-semibold text-white truncate">
                                  {app.appName}
                                </span>
                                <span className="text-[10px] font-mono text-slate-400 shrink-0">
                                  v{app.versionName}
                                </span>
                              </div>
                              <div className="text-[10px] font-mono text-slate-400 truncate">
                                {app.packageName}
                              </div>
                              <div className="flex items-center gap-2 mt-1 text-[10px] font-mono">
                                <span className="text-emerald-300">APK {app.apkSizeMb}M</span>
                                <span className="text-sky-300">Data {app.dataSizeMb}M</span>
                                {app.hasSplitApks && (
                                  <span className="px-1 rounded bg-sky-500/15 text-sky-300">
                                    Split
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </>
                  )}

                  {phoneNavTab === 'snapshots' && (
                    <div className="flex flex-col gap-2">
                      <div className="text-xs font-semibold text-slate-300 px-1">
                        Saved Zstd Archives ({snapshots.length})
                      </div>
                      {snapshots.map((snap) => (
                        <div
                          key={snap.id}
                          className="p-3 rounded-2xl bg-[#111827] border border-slate-800 flex flex-col gap-2"
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-white">{snap.appName}</span>
                            <span className="text-[10px] font-mono text-emerald-300">
                              {snap.sizeMb} MB
                            </span>
                          </div>
                          <div className="text-[10px] font-mono text-slate-400 break-all">
                            {snap.archivePath}
                          </div>
                          <div className="flex items-center justify-between pt-1 border-t border-slate-800/80">
                            <span className="text-[10px] font-mono text-slate-500">
                              SHA256: {snap.sha256.slice(0, 10)}
                            </span>
                            <div className="flex gap-1.5">
                              <button
                                onClick={() => restoreSnapshot(snap)}
                                className="px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-[10px] font-semibold flex items-center gap-1"
                              >
                                <RotateCcw className="w-3 h-3" />
                                <span>Restore</span>
                              </button>
                              <button
                                onClick={() => deleteSnapshot(snap.id)}
                                className="p-1 rounded-lg bg-slate-800 text-slate-400 hover:text-red-300"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {phoneNavTab === 'migration' && (
                    <div className="flex flex-col gap-2.5">
                      <div className="p-3 rounded-2xl bg-[#111827] border border-slate-800 flex flex-col gap-2">
                        <div className="text-xs font-bold text-sky-300 flex items-center gap-1.5">
                          <Sparkles className="w-4 h-4" />
                          <span>Custom ROM Flashable ZIP</span>
                        </div>
                        <p className="text-[11px] text-slate-400">
                          Bundles {snapshots.length} snapshots + recovery `update-binary` + SELinux `restorecon -Rv`.
                        </p>
                        <input
                          type="text"
                          value={migrationZipName}
                          onChange={(e) => setMigrationZipName(e.target.value)}
                          className="w-full bg-slate-950 border border-slate-800 rounded-xl px-2.5 py-1.5 text-[10px] font-mono text-emerald-300"
                        />
                        <button
                          onClick={generateFlashableMigrationZip}
                          className="w-full py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1.5"
                        >
                          <Layers className="w-3.5 h-3.5" />
                          <span>Build Flashable ROM ZIP</span>
                        </button>
                      </div>
                    </div>
                  )}

                  {phoneNavTab === 'shell' && (
                    <div className="flex flex-col gap-2 h-full">
                      <form onSubmit={handleRunCustomCommand} className="flex gap-1.5">
                        <input
                          type="text"
                          value={customCmd}
                          onChange={(e) => setCustomCmd(e.target.value)}
                          placeholder="su -c command..."
                          className="flex-1 bg-[#111827] border border-slate-800 rounded-xl px-2.5 py-1.5 text-xs font-mono text-slate-100"
                        />
                        <button
                          type="submit"
                          className="px-3 py-1.5 rounded-xl bg-emerald-500 text-slate-950 font-bold text-xs"
                        >
                          Run
                        </button>
                      </form>
                      <div className="flex-1 bg-slate-950 border border-slate-800 rounded-2xl p-2.5 font-mono text-[10px] overflow-y-auto flex flex-col gap-2">
                        {shellLogs.map((log) => (
                          <div key={log.id} className="border-b border-slate-900 pb-1.5">
                            <div className="text-emerald-400"># {log.command}</div>
                            <pre className="text-slate-300 whitespace-pre-wrap mt-0.5">
                              {log.output}
                            </pre>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                {/* Material 3 Bottom NavigationBar */}
                <div className="bg-[#111827] border-t border-slate-800 px-2 py-2 grid grid-cols-4 gap-1 select-none">
                  {[
                    { id: 'apps', label: 'Apps', icon: Archive },
                    { id: 'snapshots', label: 'Snapshots', icon: RotateCcw },
                    { id: 'migration', label: 'ROM Zip', icon: FolderArchive },
                    { id: 'shell', label: 'Root su', icon: Terminal },
                  ].map((item) => {
                    const Icon = item.icon;
                    const active = phoneNavTab === item.id;
                    return (
                      <button
                        key={item.id}
                        onClick={() => {
                          setPhoneNavTab(item.id as typeof phoneNavTab);
                          setActiveTab(item.id as typeof activeTab);
                        }}
                        className="flex flex-col items-center gap-1 py-1 rounded-xl transition"
                      >
                        <div
                          className={`px-4 py-1 rounded-full transition ${
                            active
                              ? 'bg-emerald-500/20 text-emerald-300'
                              : 'text-slate-400 hover:text-slate-200'
                          }`}
                        >
                          <Icon className="w-4 h-4" />
                        </div>
                        <span
                          className={`text-[10px] font-medium ${
                            active ? 'text-emerald-300 font-semibold' : 'text-slate-400'
                          }`}
                        >
                          {item.label}
                        </span>
                      </button>
                    );
                  })}
                </div>

                {/* Gesture Navigation Pill */}
                <div className="bg-[#111827] pb-2 pt-0.5 flex justify-center">
                  <div className="w-28 h-1 rounded-full bg-slate-600" />
                </div>
              </div>
            </div>

            {/* Right 7 Columns: Signed APK Builder + Live Code & Batch Controls */}
            <div className="lg:col-span-7 flex flex-col gap-4">
              {/* Signed APK Builder Card */}
              <div className="bg-[#111827] border border-slate-800 rounded-2xl p-5 flex flex-col gap-4">
                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                      <KeyRound className="w-5 h-5" />
                    </div>
                    <div>
                      <h2 className="text-base font-bold text-white">
                        Signed Release APK Builder (V1 / V2 / V3 / V4)
                      </h2>
                      <p className="text-xs text-slate-400">
                        Configured with <code className="text-emerald-300 font-mono">app/build.gradle.kts</code>,{' '}
                        <code className="text-emerald-300 font-mono">proguard-rules.pro</code> &amp; GitHub Actions
                      </p>
                    </div>
                  </div>
                  <span className="text-[11px] font-mono px-2.5 py-1 rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                    {signedBuildOutput.builtAt}
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-slate-300 font-medium block mb-1">
                      Keystore File (`storeFile`)
                    </label>
                    <input
                      type="text"
                      value={keystoreFile}
                      onChange={(e) => setKeystoreFile(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-300 font-medium block mb-1">
                      Key Alias (`keyAlias`)
                    </label>
                    <input
                      type="text"
                      value={keyAlias}
                      onChange={(e) => setKeyAlias(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-300 font-medium block mb-1">
                      Store Password
                    </label>
                    <input
                      type="text"
                      value={storePassword}
                      onChange={(e) => setStorePassword(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-emerald-300"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-300 font-medium block mb-1">
                      Key Password
                    </label>
                    <input
                      type="text"
                      value={keyPassword}
                      onChange={(e) => setKeyPassword(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-emerald-300"
                    />
                  </div>
                </div>

                {/* Signature Scheme Checkboxes */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {[
                    { label: 'V1 (JAR)', checked: v1Enabled, toggle: () => setV1Enabled((v) => !v) },
                    { label: 'V2 (APK)', checked: v2Enabled, toggle: () => setV2Enabled((v) => !v) },
                    {
                      label: 'V3 (Rotation)',
                      checked: v3Enabled,
                      toggle: () => setV3Enabled((v) => !v),
                    },
                    {
                      label: 'V4 (Stream)',
                      checked: v4Enabled,
                      toggle: () => setV4Enabled((v) => !v),
                    },
                  ].map((s) => (
                    <label
                      key={s.label}
                      onClick={s.toggle}
                      className={`p-2 rounded-lg border text-xs font-mono cursor-pointer flex items-center justify-between ${
                        s.checked
                          ? 'bg-emerald-500/15 border-emerald-500/50 text-emerald-200'
                          : 'bg-slate-900 border-slate-800 text-slate-400'
                      }`}
                    >
                      <span>{s.label}</span>
                      <input
                        type="checkbox"
                        checked={s.checked}
                        onChange={() => {}}
                        className="accent-emerald-500"
                      />
                    </label>
                  ))}
                </div>

                <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 font-mono text-xs flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">Signed Release Artifact:</span>
                    <span className="text-emerald-300 font-semibold">
                      {signedBuildOutput.apkName} ({signedBuildOutput.sizeMb})
                    </span>
                  </div>
                  <div className="text-[10px] text-slate-500 truncate">
                    SHA-256: {signedBuildOutput.sha256Fingerprint}
                  </div>
                </div>

                <div className="flex flex-wrap gap-2.5">
                  <button
                    onClick={triggerSignedApkBuild}
                    className="flex-1 py-2.5 px-4 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-xs flex items-center justify-center gap-2 shadow transition"
                  >
                    <Lock className="w-4 h-4" />
                    <span>Run Signed APK Build (`assembleRelease`)</span>
                  </button>

                  <a
                    href="/SlashBackup-Android-Signed-Project.zip"
                    download="SlashBackup-Android-Signed-Project.zip"
                    className="py-2.5 px-4 rounded-xl bg-sky-500 hover:bg-sky-400 text-slate-950 font-semibold text-xs flex items-center gap-2 shadow transition"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download Signed Android Project (.zip)</span>
                  </a>
                </div>
              </div>

              {/* Code & Build File Explorer Card */}
              <div className="bg-[#111827] border border-slate-800 rounded-2xl p-5 flex flex-col gap-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <FileCode2 className="w-4 h-4 text-emerald-400" />
                    <h3 className="text-sm font-bold text-white">
                      Kotlin + Jetpack Compose &amp; Signing Files
                    </h3>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {ANDROID_PROJECT_FILES.map((file, idx) => (
                      <button
                        key={file.path}
                        onClick={() => setSelectedFileIndex(idx)}
                        className={`px-2.5 py-1 rounded-lg text-[11px] font-mono border transition ${
                          selectedFileIndex === idx
                            ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-200'
                            : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        {file.path.split('/').pop()}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs bg-slate-900/90 px-3 py-2 rounded-lg border border-slate-800">
                  <span className="font-mono text-emerald-300 truncate">
                    {ANDROID_PROJECT_FILES[selectedFileIndex].path}
                  </span>
                  <button
                    onClick={() =>
                      copyText(
                        ANDROID_PROJECT_FILES[selectedFileIndex].path,
                        ANDROID_PROJECT_FILES[selectedFileIndex].content
                      )
                    }
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-[11px] font-mono text-slate-200 flex items-center gap-1 shrink-0"
                  >
                    {copiedPath === ANDROID_PROJECT_FILES[selectedFileIndex].path ? (
                      <>
                        <Check className="w-3 h-3 text-emerald-400" />
                        <span className="text-emerald-300">Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>

                <pre className="bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-[11px] font-mono text-slate-200 overflow-x-auto max-h-[270px] leading-relaxed">
                  {ANDROID_PROJECT_FILES[selectedFileIndex].content}
                </pre>
              </div>
            </div>
          </div>
        ) : (
          /* FULL-WIDTH DESKTOP VIEW */
          <div className="flex flex-col gap-4">
            {activeTab === 'apps' && (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
                <div className="lg:col-span-2 flex flex-col gap-3">
                  <div className="bg-[#111827] border border-slate-800 rounded-xl p-3.5 flex flex-wrap items-center justify-between gap-3">
                    <div className="relative flex-1 min-w-[200px]">
                      <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Filter packages..."
                        className="w-full bg-slate-900/90 border border-slate-700/80 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                      />
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setShowSystemApps((v) => !v)}
                        className="px-3 py-1.5 rounded-lg text-xs font-medium border bg-slate-900 border-slate-700 text-slate-300"
                      >
                        {showSystemApps ? 'Showing System Apps' : 'Hide System Apps'}
                      </button>
                      <button
                        onClick={toggleSelectAllVisible}
                        className="px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 text-slate-200 border border-slate-700"
                      >
                        Toggle All ({filteredApps.length})
                      </button>
                    </div>
                  </div>

                  <div className="flex flex-col gap-2.5">
                    {filteredApps.map((app) => (
                      <div
                        key={app.packageName}
                        onClick={() => toggleAppSelection(app.packageName)}
                        className={`cursor-pointer rounded-xl border p-3.5 transition flex items-start justify-between gap-3 ${
                          app.selected
                            ? 'bg-[#131F2E] border-emerald-500/50'
                            : 'bg-[#111827] border-slate-800/90'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          <div className="mt-0.5 text-emerald-400">
                            {app.selected ? (
                              <CheckSquare className="w-4 h-4" />
                            ) : (
                              <Square className="w-4 h-4 text-slate-500" />
                            )}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-semibold text-sm text-white">
                                {app.appName}
                              </span>
                              <span className="text-[11px] font-mono text-slate-400">
                                v{app.versionName}
                              </span>
                            </div>
                            <p className="text-xs font-mono text-slate-400 mt-0.5">
                              {app.packageName} • UID {app.uid}
                            </p>
                          </div>
                        </div>
                        <span className="text-xs font-mono text-emerald-300">
                          {(app.apkSizeMb + app.dataSizeMb).toFixed(1)} MB
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-[#111827] border border-slate-800 rounded-xl p-4 flex flex-col gap-4 h-fit">
                  <h2 className="text-sm font-bold text-white">Batch Backup Pipeline</h2>
                  <button
                    onClick={runBatchBackup}
                    disabled={selectedApps.length === 0 || !!activeOperation}
                    className="w-full py-2.5 px-4 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-xs flex items-center justify-center gap-2"
                  >
                    <Play className="w-4 h-4 fill-current" />
                    <span>Backup {selectedApps.length} Selected Apps</span>
                  </button>
                </div>
              </div>
            )}

            {activeTab === 'snapshots' && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {snapshots.map((snap) => (
                  <div
                    key={snap.id}
                    className="bg-[#111827] border border-slate-800 rounded-xl p-4 flex flex-col justify-between gap-3"
                  >
                    <div>
                      <h3 className="font-semibold text-sm text-white">{snap.appName}</h3>
                      <p className="text-xs font-mono text-slate-400">{snap.archivePath}</p>
                    </div>
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => restoreSnapshot(snap)}
                        className="px-3 py-1.5 rounded-lg bg-emerald-500/20 text-emerald-200 border border-emerald-500/40 text-xs"
                      >
                        Restore + Fix SELinux
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {(activeTab === 'migration' ||
              activeTab === 'shell' ||
              activeTab === 'signed_apk' ||
              activeTab === 'kotlin') && (
              <div className="bg-[#111827] border border-slate-800 rounded-xl p-5 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-white">
                    Switch to Split Phone Preview for Side-by-Side Android Device + Code View
                  </h3>
                  <p className="text-xs text-slate-400 mt-1">
                    Click the button on the right to see the interactive Android 15 device simulator alongside the Signed APK Builder.
                  </p>
                </div>
                <button
                  onClick={() => setViewMode('phone_split')}
                  className="px-4 py-2 rounded-xl bg-emerald-500 text-slate-950 font-semibold text-xs"
                >
                  Enable Android Phone Preview
                </button>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
