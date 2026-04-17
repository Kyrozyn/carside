package me.kyrozyn.carside.ui.garage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import me.kyrozyn.carside.core.format.formatIdr
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.FuelEconomySummary
import me.kyrozyn.carside.domain.model.FuelEntry
import me.kyrozyn.carside.domain.model.Trip

private val dateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: GarageViewModel = viewModel(factory = GarageViewModel.factory(container))
    var selectedTripId by remember { mutableStateOf<Long?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var fuelEditEntry by remember { mutableStateOf<FuelEntry?>(null) }
    var showDataTypesInfo by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    if (selectedTripId != null) {
        TripDetailPane(
            tripId = selectedTripId!!,
            container = container,
            lineColorHex = "#2962FF",
            onBack = { selectedTripId = null },
        )
        return
    }

    if (showFuelDialog) {
        FuelEntryDialog(
            initial = fuelEditEntry,
            onDismiss = { showFuelDialog = false },
            onConfirm = { entry ->
                if (entry.id == 0L) vm.addFuelEntry(entry) else vm.updateFuelEntry(entry)
                showFuelDialog = false
            },
        )
    }

    if (showDataTypesInfo) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDataTypesInfo = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            GarageDataTypesInfoSheet(onDismiss = { showDataTypesInfo = false })
        }
    }

    val trips by vm.trips.collectAsStateWithLifecycle()
    val fuelEntries by vm.fuelEntries.collectAsStateWithLifecycle()
    val economy by vm.economy.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "🚗 Garage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Catat perjalanan (GPS) & bahan bakar (odometer)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedButton(
                onClick = { showDataTypesInfo = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("ℹ️", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Bedanya dua jenis data",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = {
                        Text(
                            "🗺️ Perjalanan",
                            fontWeight = if (tabIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = {
                        Text(
                            "⛽ Bahan bakar",
                            fontWeight = if (tabIndex == 1) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
            Text(
                text = if (tabIndex == 0) {
                    "Di tab ini: hanya data dari GPS (jarak & waktu perjalanan)."
                } else {
                    "Di tab ini: data yang Anda ketik (odometer, liter, biaya) untuk km/L & total pengeluaran."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            when (tabIndex) {
                0 -> TripListPane(
                    trips = trips,
                    onTripClick = { selectedTripId = it },
                    onDeleteTrip = { vm.deleteTrip(it) },
                )
                1 -> FuelPane(
                    entries = fuelEntries,
                    economy = economy,
                    onAdd = {
                        fuelEditEntry = null
                        showFuelDialog = true
                    },
                    onEdit = {
                        fuelEditEntry = it
                        showFuelDialog = true
                    },
                    onDelete = { vm.deleteFuelEntry(it) },
                )
            }
        }
    }
}

@Composable
private fun GarageDataTypesInfoSheet(
    onDismiss: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "ℹ️ Bedanya dua jenis data",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Carside menyimpan dua hal berbeda: perjalanan dari GPS, dan catatan isi bensin dari Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Text(
            "🗺️ Tab Perjalanan (GPS)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Jarak dan waktu dihitung dari sinyal GPS — bukan angka di speedometer mobil. " +
                "Trip mulai otomatis setelah Anda jalan cukup lama, lalu berhenti setelah mobil diam atau macet ~15 menit. " +
                "Berguna untuk melihat rute perkiraan di peta.",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f,
        )
        Text(
            "⛽ Tab Bahan bakar (odometer & liter)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Anda mengetik sendiri odometer, liter, dan total bayar dari struk. " +
                "Km/L dihitung dari selisih odometer antara dua kali isi penuh (full tank), dibagi total liter pada periode itu — " +
                "tidak menggunakan jarak GPS.",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tutup")
        }
    }
}

@Composable
private fun TripListPane(
    trips: List<Trip>,
    onTripClick: (Long) -> Unit,
    onDeleteTrip: (Long) -> Unit,
) {
    if (trips.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🛣️",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    "Belum ada perjalanan GPS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aplikasi akan mendeteksi perjalanan sendiri: setelah kecepatan cukup ~10 detik, " +
                        "rekaman dimulai; setelah berhenti lama (~15 menit, untuk macet), trip disimpan. " +
                        "Pastikan izin lokasi aktif dan Anda sedang mengemudi. " +
                        "Jarak yang tercatat adalah perkiraan dari GPS, bukan odometer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(trips, key = { it.id }) { trip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTripClick(trip.id) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Waktu mulai (perkiraan)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "📅 ${dateFmt.format(Instant.ofEpochMilli(trip.startedAtEpochMs))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatChip(
                                emoji = "🛣️",
                                caption = "Jarak total (GPS)",
                                value = formatKm(trip.distanceMeters),
                            )
                            StatChip(
                                emoji = "⏱️",
                                caption = "Lama jalan",
                                value = formatDurationShort(trip.durationSeconds),
                            )
                        }
                        trip.maxSpeedMps?.let { mps ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "💨 Kecepatan tertinggi (GPS): ${"%.0f".format(mps * 3.6f)} km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "Ketuk → lihat garis rute di peta (titik dari GPS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    IconButton(
                        onClick = { onDeleteTrip(trip.id) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus trip",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, caption: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = "$emoji $caption",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatDurationShort(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}j ${m}m" else "${m} mnt"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDetailPane(
    tripId: Long,
    container: AppContainer,
    lineColorHex: String,
    onBack: () -> Unit,
) {
    val trip by container.tripRepository.observeTrip(tripId)
        .collectAsStateWithLifecycle(initialValue = null)
    val points by container.tripRepository.observePoints(tripId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🗺️ Detail perjalanan",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val t = trip
            if (t == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏳", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Memuat data trip…",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Jika lama, periksa apakah trip masih ada di daftar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
                return@Column
            }
            TripRouteMap(
                points = points,
                lineColorHex = lineColorHex,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            Text(
                text = "Garis di peta menyambungkan titik lokasi GPS. Bukan pengukuran odometer mobil; " +
                    "akurasi tergantung sinyal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "📊 Ringkasan (semua dari GPS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Angka di bawah dihitung dari data lokasi & kecepatan GPS pada trip ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    DetailRow(
                        label = "🛣️ Jarak total",
                        value = formatKm(t.distanceMeters),
                        hint = "Penjumlahan jarak antar titik GPS",
                    )
                    DetailRow(
                        label = "⏱️ Durasi",
                        value = formatDuration(t.durationSeconds),
                        hint = "Dari waktu mulai sampai trip dianggap selesai",
                    )
                    t.avgSpeedMps?.let { mps ->
                        DetailRow(
                            label = "📈 Kecepatan rata-rata",
                            value = "${"%.1f".format(mps * 3.6f)} km/h",
                            hint = "Jarak total dibagi durasi (perkiraan)",
                        )
                    }
                    t.maxSpeedMps?.let { mps ->
                        DetailRow(
                            label = "💨 Kecepatan tertinggi",
                            value = "${"%.0f".format(mps * 3.6f)} km/h",
                            hint = "Nilai tertinggi yang dilaporkan GPS selama trip",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    hint: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FuelPane(
    entries: List<FuelEntry>,
    economy: FuelEconomySummary,
    onAdd: () -> Unit,
    onEdit: (FuelEntry) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                EconomySummaryCard(economy)
            }
            if (entries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("⛽", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Belum ada catatan BBM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Catat setiap kali isi bensin: odometer (km), liter, dan total bayar. " +
                                    "Untuk km/L yang bermakna, centang “full tank” pada isi penuh; " +
                                    "aplikasi menggabungkan isian antara dua full tank untuk menghitung jarak & liter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            items(entries, key = { it.id }) { e ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEdit(e) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Waktu pencatatan (perangkat)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "📅",
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text(
                                    dateFmt.format(Instant.ofEpochMilli(e.filledAtEpochMs)),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Angka odometer (km) saat isi: ${"%.1f".format(e.odometerKm)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Bensin yang masuk tangki: ${"%.2f".format(e.liters)} liter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Total dibayar: ${formatIdr(e.totalCostIdr)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (e.isFullTank) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                ) {
                                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(
                                            "✅ Dicatat sebagai isi penuh",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "Dipakai untuk hitungan km/L antar full tank",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { onEdit(e) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(
                                onClick = { onDelete(e.id) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            icon = {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
            },
            text = { Text("⛽ Catat isi bensin") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun EconomySummaryCard(economy: FuelEconomySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "📈 Konsumsi bahan bakar (dari data odometer Anda)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Km/L tidak dari GPS. Rumus: (odometer akhir − odometer awal) ÷ " +
                    "(jumlah liter pada isian dari setelah awal sampai isi penuh berikutnya).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            economy.latestKmPerLiter?.let {
                Column {
                    Text(
                        "🎯 Km/L pada segmen terakhir yang lengkap",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${"%.2f".format(it)} km/L",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Segmen = dari satu isi penuh ke isi penuh berikutnya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } ?: Text(
                "🎯 Km/L belum bisa dihitung: perlu minimal dua kali isi berturut-turut yang ditandai sebagai penuh (full tank).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            economy.rollingAvgKmPerLiter?.let {
                Column {
                    Text(
                        "📊 Rata-rata semua segmen full-to-full",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${"%.2f".format(it)} km/L",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
            Text(
                "💵 Akumulasi semua catatan: ${formatIdr(economy.totalSpentIdr)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "🛢️ Total liter yang pernah Anda catat: ${"%.1f".format(economy.totalLiters)} L",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            economy.totalDistanceKm?.let { d ->
                Text(
                    "📏 Selisih odometer (entri terakhir − pertama): ${"%.1f".format(d)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            economy.pricePerKmIdr?.let { p ->
                Text(
                    "💸 Perkiraan biaya per km: ${formatIdr(p.toLong())} / km (total biaya ÷ selisih odometer di atas)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatKm(meters: Double): String = "%.2f km".format(meters / 1000.0)

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d jam %02d m %02d dtk".format(h, m, s)
    } else {
        "%d m %02d dtk".format(m, s)
    }
}
