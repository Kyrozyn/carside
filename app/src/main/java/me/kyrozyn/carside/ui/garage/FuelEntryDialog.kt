package me.kyrozyn.carside.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.kyrozyn.carside.domain.model.FuelEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelEntryDialog(
    initial: FuelEntry?,
    onDismiss: () -> Unit,
    onConfirm: (FuelEntry) -> Unit,
) {
    val isEdit = initial != null && initial.id != 0L
    val key = initial?.id ?: -1L
    var odometerText by remember(key) { mutableStateOf(initial?.odometerKm?.toString() ?: "") }
    var litersText by remember(key) { mutableStateOf(initial?.liters?.toString() ?: "") }
    var costText by remember(key) { mutableStateOf(initial?.totalCostIdr?.toString() ?: "") }
    var fullTank by remember(key) { mutableStateOf(initial?.isFullTank ?: false) }
    var notes by remember(key) { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isEdit) "✏️ Ubah catatan isi bensin" else "⛽ Catat isi bensin baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Semua angka di bawah Anda isi manual dari mobil & struk. " +
                        "Ini dipakai untuk km/L dan biaya — tidak diambil dari GPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        text = {
            val scroll = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
            ) {
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("📏 Odometer (kilometer)") },
                    supportingText = {
                        Text(
                            "Angka total jarak di speedometer mobil saat Anda mengisi BBM (bukan dari aplikasi).",
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = it },
                    label = { Text("🛢️ Volume bensin (liter)") },
                    supportingText = {
                        Text(
                            "Banyaknya liter yang tertera di mesin pompa atau struk. Pakai titik atau koma untuk desimal.",
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("💰 Total yang dibayar (Rupiah)") },
                    supportingText = {
                        Text(
                            "Jumlah uang di struk untuk transaksi ini saja. Tulis angka utuh, misalnya 350000.",
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "✅ Tangki diisi sampai penuh (full tank)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Nyalakan hanya jika benar-benar full. Dua isi penuh berturut-turut " +
                                    "membolehkan aplikasi menghitung km/L dari selisih odometer ÷ liter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = fullTank, onCheckedChange = { fullTank = it })
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("📝 Catatan tambahan (opsional)") },
                    supportingText = {
                        Text(
                            "Contoh: SPBU, jenis Pertamax, atau kondisi macet. Tidak mempengaruhi perhitungan.",
                        )
                    },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val odo = odometerText.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                    val liters = litersText.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                    val cost = costText.toLongOrNull() ?: return@TextButton
                    if (odo <= 0.0 || liters <= 0.0 || cost < 0L) return@TextButton
                    val entry = FuelEntry(
                        id = initial?.id ?: 0L,
                        filledAtEpochMs = initial?.filledAtEpochMs ?: System.currentTimeMillis(),
                        odometerKm = odo,
                        liters = liters,
                        totalCostIdr = cost,
                        isFullTank = fullTank,
                        notes = notes.takeIf { it.isNotBlank() },
                    )
                    onConfirm(entry)
                },
            ) {
                Text("💾 Simpan ke daftar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}
