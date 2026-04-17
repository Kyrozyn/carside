package me.kyrozyn.carside

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
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
import me.kyrozyn.carside.core.isCarsideNotificationListenerEnabled
import me.kyrozyn.carside.core.requestCarsideNotificationListenerRebind
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kyrozyn.carside.ui.apps.AppDrawerScreen
import me.kyrozyn.carside.ui.dashboard.DashboardScreen
import me.kyrozyn.carside.ui.dashboard.DashboardViewModel
import me.kyrozyn.carside.ui.internetradio.InternetRadioScreen
import me.kyrozyn.carside.ui.garage.GarageScreen
import me.kyrozyn.carside.ui.offlinemaps.AddOfflineMapScreen
import me.kyrozyn.carside.ui.offlinemaps.OfflineMapsScreen
import me.kyrozyn.carside.ui.radio.RadioScreen
import me.kyrozyn.carside.ui.theme.CarsideTheme

class MainActivity : ComponentActivity() {

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
            CarsideTheme {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                val internetRadioState by container.internetRadioController.state.collectAsStateWithLifecycle()
                val offlineDownloadActive by container.offlineMapsRepository.activeDownload.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                var notificationListenerEnabled by remember { mutableStateOf(false) }
                var showNotificationAccessDialog by remember { mutableStateOf(false) }
                var showAppDrawer by remember { mutableStateOf(false) }
                var showRadio by remember { mutableStateOf(false) }
                var showInternetRadio by remember { mutableStateOf(false) }
                var showOfflineMaps by remember { mutableStateOf(false) }
                var showOfflineMapAdd by remember { mutableStateOf(false) }
                var showGarage by remember { mutableStateOf(false) }

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
                        onLaunchApp = { packageName ->
                            packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                                launchIntent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                                )
                                startActivity(launchIntent)
                            }
                        },
                    )
                } else if (showRadio) {
                    RadioScreen(
                        container = container,
                        onBack = { showRadio = false },
                    )
                } else if (showInternetRadio) {
                    InternetRadioScreen(
                        container = container,
                        onBack = { showInternetRadio = false },
                    )
                } else if (showGarage) {
                    GarageScreen(
                        container = container,
                        onBack = { showGarage = false },
                    )
                } else if (showOfflineMaps) {
                    if (showOfflineMapAdd) {
                        AddOfflineMapScreen(
                            container = container,
                            onBack = { showOfflineMapAdd = false },
                        )
                    } else {
                        OfflineMapsScreen(
                            container = container,
                            onBack = {
                                showOfflineMaps = false
                                showOfflineMapAdd = false
                            },
                            onNavigateToAdd = { showOfflineMapAdd = true },
                        )
                    }
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
                        onOpenAppDrawer = { showAppDrawer = true },
                        onOpenRadio = { showRadio = true },
                        onOpenInternetRadio = { showInternetRadio = true },
                        onOpenOfflineMaps = { showOfflineMaps = true },
                        onOpenGarage = { showGarage = true },
                        onStopTrip = vm::stopActiveTrip,
                        internetRadioState = internetRadioState,
                        onStopInternetRadio = { container.internetRadioController.stop() },
                        onSetUseGpsForwardMapBearing = vm::setUseGpsForwardMapBearing,
                        onLaunchShortcutApp = { packageName ->
                            packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                                launchIntent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                                )
                                startActivity(launchIntent)
                            }
                        },
                        onOpenShortcutPicker = vm::openShortcutPicker,
                        onDismissShortcutPicker = vm::closeShortcutPicker,
                        onSelectShortcutApp = vm::setShortcutPackage,
                        onOpenWeatherDetails = vm::openWeatherDetails,
                        onCloseWeatherDetails = vm::closeWeatherDetails,
                        offlineDownloadActive = offlineDownloadActive,
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

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
