package me.kyrozyn.carside.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/** Panjang maks hanya untuk segmen nama jalan di FAB (kecamatan & kota tidak dipotong). */
const val FAB_STREET_MAX_CHARS = 30

/**
 * Bagian alamat untuk FAB: jalan (bisa dipendekkan), kecamatan & kota utuh.
 */
data class StreetAddressInfo(
    val street: String,
    val kecamatan: String?,
    val kota: String?,
) {
    /**
     * Teks FAB: hanya jalan yang dipotong (maks ~18 karakter).
     * Urutan tetap: **jalan · kecamatan · kota** (kota selalu di ujung).
     */
    fun fabLabel(): String {
        val s = street.trim()
        val streetPart = when {
            s.isBlank() || s == "—" -> ""
            s.length <= 18 -> s
            else -> s.take(17) + "…"
        }
        val place = listOfNotNull(kecamatan?.takeIf { it.isNotBlank() }, kota?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
        return when {
            streetPart.isEmpty() && place.isEmpty() -> "—"
            streetPart.isEmpty() -> place
            place.isEmpty() -> streetPart
            else -> "$streetPart · $place"
        }
    }
}

/** Satu baris alamat lengkap (untuk bottom sheet) + bagian untuk FAB. */
data class ReverseGeocodeResult(
    val fullAddressLine: String,
    val fabParts: StreetAddressInfo,
)

/**
 * Reverse geocoding sekali jalan: alamat lengkap seperti [Address.getAddressLine] / rangkaian field,
 * plus bagian terstruktur untuk label FAB.
 */
suspend fun reverseGeocodeResult(
    context: Context,
    latitude: Double,
    longitude: Double,
): ReverseGeocodeResult? = withContext(Dispatchers.IO) {
    val address = fetchAddress(context, latitude, longitude) ?: return@withContext null
    val full = address.toReadableAddress() ?: return@withContext null
    val fabParts = address.toStreetAddressInfo()
    ReverseGeocodeResult(fullAddressLine = full, fabParts = fabParts)
}

/** @deprecated Gunakan [reverseGeocodeResult]; tetap ada jika hanya butuh bagian FAB. */
suspend fun reverseGeocodeAddressInfo(
    context: Context,
    latitude: Double,
    longitude: Double,
): StreetAddressInfo? = reverseGeocodeResult(context, latitude, longitude)?.fabParts

private suspend fun fetchAddress(
    context: Context,
    latitude: Double,
    longitude: Double,
): Address? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                },
            )
        }
    } else {
        try {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }
}

/** Alamat satu baris lengkap (seperti sebelum ada FAB terstruktur). */
private fun Address.toReadableAddress(): String? {
    getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    return buildString {
        val street = when {
            !thoroughfare.isNullOrBlank() -> thoroughfare
            !featureName.isNullOrBlank() && featureName != subLocality -> featureName
            else -> null
        }
        if (!street.isNullOrBlank()) {
            append(street)
            if (!subThoroughfare.isNullOrBlank()) append(" $subThoroughfare")
        }
        if (!locality.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(locality)
        }
        if (!subAdminArea.isNullOrBlank() && subAdminArea != locality) {
            if (isNotEmpty()) append(", ")
            append(subAdminArea)
        }
        if (!countryName.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(countryName)
        }
    }.trim().takeIf { it.isNotBlank() }
}

private fun Address.toStreetAddressInfo(): StreetAddressInfo {
    val street = streetLineOnly()
    val (kecamatan, kota) = extractKecamatanKotaMerged()
    return StreetAddressInfo(
        street = street.ifBlank { "—" },
        kecamatan = kecamatan?.let { stripWilayahPrefix(it) },
        kota = kota?.let { stripWilayahPrefix(it) },
    )
}

/** Hanya baris jalan/nomor untuk FAB. */
private fun Address.streetLineOnly(): String {
    val tf = thoroughfare?.trim()
    val st = subThoroughfare?.trim()
    val fromFields = buildString {
        if (!tf.isNullOrBlank()) {
            append(tf)
            if (!st.isNullOrBlank()) append(" $st")
        }
    }.trim()
    if (fromFields.isNotBlank()) return fromFields
    val line0 = getAddressLine(0)?.trim() ?: return ""
    if (line0.isBlank()) return ""
    val first = line0.split(",").firstOrNull()?.trim()
    return first ?: line0
}

/** Gabungkan field Android + parse dari baris alamat lengkap (sering hanya terisi di getAddressLine(0)). */
private fun Address.extractKecamatanKotaMerged(): Pair<String?, String?> {
    val fromFields = extractKecamatanKotaFromFields()
    val line0 = getAddressLine(0)?.trim()
    val fromLine = if (!line0.isNullOrBlank()) parseKecamatanKotaFromLine0(line0) else null
    val kecamatan = fromFields.first ?: fromLine?.first
    val kota = fromFields.second ?: fromLine?.second
    return kecamatan?.takeIf { it.isNotBlank() } to kota?.takeIf { it.isNotBlank() }
}

private fun Address.extractKecamatanKotaFromFields(): Pair<String?, String?> {
    val loc = locality?.trim()?.takeIf { it.isNotEmpty() }
    val subAdmin = subAdminArea?.trim()?.takeIf { it.isNotEmpty() }
    val subLoc = subLocality?.trim()?.takeIf { it.isNotEmpty() }

    val kota = loc ?: subAdmin
    val kecamatan = when {
        subAdmin != null && kota != null && !subAdmin.equals(kota, ignoreCase = true) -> subAdmin
        subLoc != null && kota != null && !subLoc.equals(kota, ignoreCase = true) -> subLoc
        else -> null
    }
    return kecamatan to kota
}

/**
 * Dari alamat berkoma (sama seperti detail): setelah jalan, ambil kecamatan & kota
 * sebelum provinsi/kode pos/negara.
 */
private fun parseKecamatanKotaFromLine0(line0: String): Pair<String?, String?> {
    val parts = line0.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 2) return null to null
    var tail = parts.drop(1).toMutableList()
    if (tail.isEmpty()) return null to null
    if (tail.last().equals("Indonesia", ignoreCase = true)) tail.removeAt(tail.lastIndex)
    if (tail.isEmpty()) return null to null
    while (tail.isNotEmpty() && looksLikeProvinceOrPostalTail(tail.last())) {
        tail.removeAt(tail.lastIndex)
    }
    if (tail.isEmpty()) return null to null
    if (tail.size == 1) return null to stripWilayahPrefix(tail[0])
    val left = tail[tail.size - 2]
    val right = tail.last()
    return orderKecamatanKotaFromTwoSegments(left, right)
}

/**
 * Dua segmen berurutan sebelum provinsi: biasanya **kiri = kecamatan**, **kanan = kota**.
 * Jika ada prefiks yang menunjukkan urutan terbalik (Kota/Kab di kiri, Kecamatan di kanan), ditukar.
 */
private fun orderKecamatanKotaFromTwoSegments(left: String, right: String): Pair<String?, String?> {
    val a = left.trim()
    val b = right.trim()
    if (a.isEmpty()) return null to stripWilayahPrefix(b)
    if (b.isEmpty()) return stripWilayahPrefix(a) to null

    val aKota = a.startsWith("Kota ", ignoreCase = true) || a.startsWith("Kota Administrasi", ignoreCase = true)
    val bKota = b.startsWith("Kota ", ignoreCase = true) || b.startsWith("Kota Administrasi", ignoreCase = true)
    val aKab = a.startsWith("Kabupaten ", ignoreCase = true) || a.startsWith("Kab. ", ignoreCase = true)
    val bKab = b.startsWith("Kabupaten ", ignoreCase = true) || b.startsWith("Kab. ", ignoreCase = true)
    val aKec = a.startsWith("Kecamatan ", ignoreCase = true) || a.startsWith("Kec. ", ignoreCase = true)
    val bKec = b.startsWith("Kecamatan ", ignoreCase = true) || b.startsWith("Kec. ", ignoreCase = true)

    val standardKecThenKota = aKec && (bKota || bKab)
    val swappedInSource = (aKota || aKab) && bKec

    val (kec, kota) = when {
        standardKecThenKota -> stripWilayahPrefix(a) to stripWilayahPrefix(b)
        swappedInSource -> stripWilayahPrefix(b) to stripWilayahPrefix(a)
        else -> stripWilayahPrefix(a) to stripWilayahPrefix(b)
    }
    return kec to kota
}

private fun looksLikeProvinceOrPostalTail(s: String): Boolean {
    val t = s.trim()
    if (t.equals("Indonesia", ignoreCase = true)) return true
    if (Regex("\\b\\d{5}\\b").containsMatchIn(t)) return true
    val lower = t.lowercase(Locale.forLanguageTag("id-ID"))
    return listOf(
        "jawa barat", "jawa timur", "jawa tengah", "dki jakarta", "jakarta",
        "yogyakarta", "bali", "ntb", "ntt", "nusa tenggara", "sumatera", "sumatra",
        "kalimantan", "sulawesi", "papua", "maluku", "aceh", "bengkulu", "jambi",
        "lampung", "riau", "kepulauan riau", "bangka belitung", "banten", "gorontalo",
        "kalimantan barat", "kalimantan timur", "kalimantan selatan", "kalimantan utara",
        "kalimantan tengah", "sulawesi utara", "sulawesi selatan", "sulawesi tengah",
        "sulawesi tenggara", "sulawesi barat", "papua barat", "maluku utara",
    ).any { lower.contains(it) }
}

/** Hilangkan kata "Kota"/"Kabupaten" di depan; tampilkan langsung nama wilayah. */
private fun stripWilayahPrefix(s: String): String {
    var t = s.trim()
    val prefixes = listOf(
        "Kota Administrasi ",
        "Kota ",
        "Kabupaten ",
        "Kab. ",
        "Kecamatan ",
        "Kec. ",
    )
    for (p in prefixes) {
        if (t.startsWith(p, ignoreCase = true)) {
            t = t.removePrefix(p).trim()
            break
        }
    }
    return t
}
