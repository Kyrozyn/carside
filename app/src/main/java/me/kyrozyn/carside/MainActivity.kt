package me.kyrozyn.carside

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.kyrozyn.carside.core.BluetoothMusicHelper
import me.kyrozyn.carside.core.isCarsideNotificationListenerEnabled
import me.kyrozyn.carside.core.requestCarsideNotificationListenerRebind
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kyrozyn.carside.ui.apps.AppDrawerScreen
import me.kyrozyn.carside.ui.internetradio.InternetRadioScreen
import me.kyrozyn.carside.ui.dashboard.DashboardScreen
import me.kyrozyn.carside.ui.dashboard.DashboardViewModel
import me.kyrozyn.carside.ui.garage.GarageScreen
import me.kyrozyn.carside.ui.logs.LogsScreen
import me.kyrozyn.carside.ui.offlinemaps.AddOfflineMapScreen
import me.kyrozyn.carside.ui.offlinemaps.OfflineMapsScreen
import me.kyrozyn.carside.ui.settings.SettingsScreen
import me.kyrozyn.carside.ui.theme.CarsideTheme

class MainActivity : ComponentActivity() {

    private var resetToHomeAction: (() -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetToHomeAction?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()
        val container = (application as CarsideApplication).container
        setContent {
            val selectedPalette by container.settingsRepository.selectedPalette.collectAsStateWithLifecycle()
            CarsideTheme(palette = selectedPalette) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                val internetRadioState by container.internetRadioController.state.collectAsStateWithLifecycle()
                val offlineDownloadActive by container.offlineMapsRepository.activeDownload.collectAsStateWithLifecycle()
                val offlineRegions by container.offlineMapsRepository.regions.collectAsStateWithLifecycle()
                val speedWarningEnabled by container.settingsRepository.speedWarningEnabled.collectAsStateWithLifecycle()
                val locationDotSize by container.settingsRepository.locationDotSize.collectAsStateWithLifecycle()
                val locationRefreshIntervalMs by container.settingsRepository.locationRefreshIntervalMs.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                
                val hasOfflineMaps = offlineRegions.isNotEmpty()

                LaunchedEffect(Unit) {
                    container.offlineMapsRepository.refreshRegions()
                }

                var notificationListenerEnabled by remember { mutableStateOf(false) }
                var showNotificationAccessDialog by remember { mutableStateOf(false) }
                var showAppDrawer by remember { mutableStateOf(false) }
                var showInternetRadio by remember { mutableStateOf(false) }
                var showGarage by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var showLogs by remember { mutableStateOf(false) }
                var settingsOfflineMaps by remember { mutableStateOf(false) }
                var settingsOfflineMapAdd by remember { mutableStateOf(false) }

                val resetToHome = {
                    showAppDrawer = false
                    showInternetRadio = false
                    showGarage = false
                    showSettings = false
                    showLogs = false
                    settingsOfflineMaps = false
                    settingsOfflineMapAdd = false
                    vm.resetOverlays()
                }
                resetToHomeAction = resetToHome

                DisposableEffect(lifecycleOwner) {
                    val syncAccess: () -> Unit = {
                        val ok = context.isCarsideNotificationListenerEnabled()
                        notificationListenerEnabled = ok
                        if (!ok) {
                            showNotificationAccessDialog = true
                        } else {
                            showNotificationAccessDialog = false
                        }
                    }
                    syncAccess()
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            syncAccess()
                            container.mediaController.refreshActiveSessions()
                            container.navigationRepository.refresh()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (showNotificationAccessDialog && !notificationListenerEnabled) {
                    AlertDialog(
                        onDismissRequest = { showNotificationAccessDialog = false },
                        title = { Text("Akses notifikasi diperlukan") },
                        text = {
                            Text(
                                "Untuk menampilkan lagu yang sedang diputar dari aplikasi lain (Spotify, YouTube Music, dll.), aktifkan akses notifikasi untuk Carside di layar berikutnya.",
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showNotificationAccessDialog = false
                                    requestCarsideNotificationListenerRebind()
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                            ) {
                                Text("Buka pengaturan")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationAccessDialog = false }) {
                                Text("Nanti")
                            }
                        },
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    vm.setLocationPermissionGranted(granted)
                }

                LaunchedEffect(Unit) {
                    val fine = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                    val coarse = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                    when {
                        fine == PackageManager.PERMISSION_GRANTED ||
                            coarse == PackageManager.PERMISSION_GRANTED -> {
                            vm.setLocationPermissionGranted(true)
                        }
                        else -> {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    }
                }

                if (showAppDrawer) {
                    AppDrawerScreen(
                        onBack = { showAppDrawer = false },
                        onHome = resetToHome,
                        onLaunchApp = { target -> launchAppOrTarget(target) },
                    )
                } else if (showInternetRadio) {
                    InternetRadioScreen(
                        container = container,
                        onBack = { showInternetRadio = false },
                        onHome = resetToHome,
                    )
                } else if (showGarage) {
                    GarageScreen(
                        container = container,
                        onBack = { showGarage = false },
                        onHome = resetToHome,
                    )
                } else if (showSettings) {
                    if (settingsOfflineMaps) {
                        if (settingsOfflineMapAdd) {
                            AddOfflineMapScreen(
                                container = container,
                                onBack = { settingsOfflineMapAdd = false },
                                onHome = resetToHome,
                            )
                        } else {
                            OfflineMapsScreen(
                                container = container,
                                onBack = {
                                    settingsOfflineMaps = false
                                    settingsOfflineMapAdd = false
                                },
                                onHome = resetToHome,
                                onNavigateToAdd = { settingsOfflineMapAdd = true },
                            )
                        }
                    } else {
                        SettingsScreen(
                            onBack = {
                                showSettings = false
                                settingsOfflineMaps = false
                                settingsOfflineMapAdd = false
                            },
                            onHome = resetToHome,
                            onKelolaDownloadMaps = { settingsOfflineMaps = true },
                            onOpenLauncherSettings = {
                                val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS)
                                if (homeIntent.resolveActivity(packageManager) != null) {
                                    startActivity(homeIntent)
                                } else {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            onOpenPrivacyPolicy = {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://carside.kyrozyn.web.id/carside-privacy-policy.html"),
                                )
                                if (browserIntent.resolveActivity(packageManager) != null) {
                                    startActivity(browserIntent)
                                }
                            },
                            speedWarningEnabled = speedWarningEnabled,
                            onSpeedWarningEnabledChange = {
                                container.settingsRepository.setSpeedWarningEnabled(it)
                            },
                            onTestSpeedWarningSound = { container.softSpeedTonePlayer.play() },
                            selectedPalette = selectedPalette,
                            onPaletteSelected = { container.settingsRepository.setSelectedPalette(it) },
                            settingsRepository = container.settingsRepository,
                            onOpenLogs = { showLogs = true },
                        )
                    }
                } else if (showLogs) {
                    LogsScreen(
                        onBack = { showLogs = false },
                        onHome = resetToHome,
                    )
                } else {
                    DashboardScreen(
                        uiState = uiState,
                        notificationListenerEnabled = notificationListenerEnabled,
                        onBrowseClick = { vm.setBrowseOpen(true) },
                        onDismissBrowse = { vm.setBrowseOpen(false) },
                        onOpenNotificationAccessSettings = {
                            showNotificationAccessDialog = false
                            requestCarsideNotificationListenerRebind()
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onRefreshSystemMedia = {
                            (application as CarsideApplication).container.mediaController.refreshActiveSessions()
                        },
                        onPlayPause = vm::onPlayPause,
                        onNext = vm::onNext,
                        onPrevious = vm::onPrevious,
                        onSeekTo = vm::onSeekTo,
                        onOpenBluetoothMusic = {
                            BluetoothMusicHelper.launch(this, container.settingsRepository)
                        },
                        onOpenAppDrawer = { showAppDrawer = true },
                        onOpenInternetRadio = { showInternetRadio = true },
                        onOpenSettings = { showSettings = true },
                        onOpenGarage = { showGarage = true },
                        internetRadioState = internetRadioState,
                        onStopInternetRadio = { container.internetRadioController.stop() },
                        onSetUseGpsForwardMapBearing = vm::setUseGpsForwardMapBearing,
                        onLaunchShortcutApp = { target -> launchAppOrTarget(target) },
                        onOpenShortcutPicker = vm::openShortcutPicker,
                        onDismissShortcutPicker = vm::closeShortcutPicker,
                        onSelectShortcutApp = vm::setShortcutPackage,
                        onOpenWeatherDetails = vm::openWeatherDetails,
                        onCloseWeatherDetails = vm::closeWeatherDetails,
                        onOpenLyricsFullscreen = vm::openLyricsFullscreen,
                        onCloseLyricsFullscreen = vm::closeLyricsFullscreen,
                        offlineDownloadActive = offlineDownloadActive,
                        hasOfflineMaps = hasOfflineMaps,
                        locationDotSize = locationDotSize,
                        locationRefreshIntervalMs = locationRefreshIntervalMs,
                        autoNavigationManager = container.autoNavigationManager,
                        onSwitchToMapLibre = { vm.setAutoMapEngine(me.kyrozyn.carside.data.settings.AutoMapEngine.MAPLIBRE) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun launchAppOrTarget(target: String) {
        if (target.contains("/")) {
            val parts = target.split("/", limit = 2)
            val pkg = parts[0]
            val cls = parts[1]
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(pkg, cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        val pkg = target.substringBefore("/")
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launchIntent)
        } else {
            BluetoothMusicHelper.launchTarget(this, target)
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
