package me.kyrozyn.carside.core.format

import java.text.NumberFormat
import java.util.Locale

private val idrFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
}

/** Format Rupiah, contoh: Rp85.000 */
fun formatIdr(amount: Long): String = idrFormat.format(amount)
