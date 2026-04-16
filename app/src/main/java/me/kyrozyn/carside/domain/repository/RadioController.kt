package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.StateFlow
import me.kyrozyn.carside.domain.model.RadioBand
import me.kyrozyn.carside.domain.model.RadioState

/**
 * Kontroler radio FM/AM hardware. Implementasi hardware menggunakan
 * `android.hardware.radio.RadioManager` dan hanya berfungsi saat APK
 * diinstal sebagai system/privileged app dan device punya BroadcastRadio HAL.
 *
 * Fallback [me.kyrozyn.carside.data.radio.NoOpRadioController] digunakan
 * saat service/permission tidak tersedia.
 */
interface RadioController {
    val state: StateFlow<RadioState>

    fun open()
    fun close()

    fun setBand(band: RadioBand)
    fun tune(frequencyKhz: Int)
    fun step(up: Boolean)
    fun scan(up: Boolean)
    fun cancelScan()

    fun setMuted(muted: Boolean)

    fun savePreset(slot: Int)
    fun loadPreset(slot: Int)
    fun clearPreset(slot: Int)
}
