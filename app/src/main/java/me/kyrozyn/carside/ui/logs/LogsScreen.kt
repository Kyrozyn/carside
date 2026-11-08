package me.kyrozyn.carside.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.kyrozyn.carside.core.AppLogger
import me.kyrozyn.carside.core.LogEntry
import me.kyrozyn.carside.core.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val allLogs by AppLogger.logs.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredLogs = remember(allLogs, selectedFilter, searchQuery) {
        allLogs.reversed().filter { entry ->
            val matchLevel = selectedFilter == null || entry.level == selectedFilter
            val matchQuery = searchQuery.isBlank() ||
                entry.tag.contains(searchQuery, ignoreCase = true) ||
                entry.message.contains(searchQuery, ignoreCase = true) ||
                (entry.throwable?.contains(searchQuery, ignoreCase = true) == true)
            matchLevel && matchQuery
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Bersihkan Log?") },
            text = { Text("Seluruh catatan log saat ini akan dihapus dari memori aplikasi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppLogger.clear()
                        showClearConfirm = false
                    },
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Batal")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Log Aplikasi & Debug", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${allLogs.size} entri log",
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
                actions = {
                    IconButton(
                        onClick = {
                            val text = AppLogger.getAllAsText()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Carside Logs", text))
                            Toast.makeText(context, "Log berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin Semua Log")
                    }
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Bersihkan Log")
                    }
                    IconButton(onClick = onHome) {
                        Icon(
                            Icons.Outlined.Home,
                            contentDescription = "Layar Utama / Home",
                            tint = MaterialTheme.colorScheme.primary,
                        )
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
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 14.dp),
        ) {
            // Search Bar & Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari di log (tag / pesan)...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Semua (${allLogs.size})") },
                )
                FilterChip(
                    selected = selectedFilter == LogLevel.ERROR,
                    onClick = {
                        selectedFilter = if (selectedFilter == LogLevel.ERROR) null else LogLevel.ERROR
                    },
                    label = { Text("Error (${allLogs.count { it.level == LogLevel.ERROR }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                )
                FilterChip(
                    selected = selectedFilter == LogLevel.WARN,
                    onClick = {
                        selectedFilter = if (selectedFilter == LogLevel.WARN) null else LogLevel.WARN
                    },
                    label = { Text("Warning (${allLogs.count { it.level == LogLevel.WARN }})") },
                )
                FilterChip(
                    selected = selectedFilter == LogLevel.INFO,
                    onClick = {
                        selectedFilter = if (selectedFilter == LogLevel.INFO) null else LogLevel.INFO
                    },
                    label = { Text("Info (${allLogs.count { it.level == LogLevel.INFO }})") },
                )
                FilterChip(
                    selected = selectedFilter == LogLevel.DEBUG,
                    onClick = {
                        selectedFilter = if (selectedFilter == LogLevel.DEBUG) null else LogLevel.DEBUG
                    },
                    label = { Text("Debug (${allLogs.count { it.level == LogLevel.DEBUG }})") },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (allLogs.isEmpty()) "Belum ada catatan log." else "Tidak ada log yang cocok dengan filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredLogs, key = { it.id }) { logItem ->
                        LogItemCard(entry = logItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemCard(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val levelColor = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFE53935)
        LogLevel.WARN -> Color(0xFFFB8C00)
        LogLevel.INFO -> Color(0xFF43A047)
        LogLevel.DEBUG -> Color(0xFF757575)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Level Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = levelColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = entry.level.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = levelColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Tag Badge
                    Text(
                        text = "[${entry.tag}]",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = entry.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!entry.throwable.isNullOrBlank()) {
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                    ) {
                        Text(
                            text = entry.throwable,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF8A80),
                        )
                    }
                }
                if (!expanded) {
                    Text(
                        text = "▼ Ketuk untuk melihat stack trace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
