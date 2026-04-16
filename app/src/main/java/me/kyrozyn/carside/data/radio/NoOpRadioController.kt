package me.kyrozyn.carside.data.radio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.kyrozyn.carside.domain.model.RadioBand
import me.kyrozyn.carside.domain.model.RadioPreset
import me.kyrozyn.carside.domain.model.RadioState
import me.kyrozyn.carside.domain.repository.RadioController

/**
 * Fallback [RadioController] saat `RadioManager` tidak tersedia atau
 * aplikasi tidak mempunyai permission system. Semua aksi adalah no-op
 * dan UI akan menampilkan state "Radio tidak tersedia".
 */
class NoOpRadioController(
    initialPresets: List<RadioPreset> = emptyList(),
    private val onPresetsChanged: ((List<RadioPreset>) -> Unit)? = null,
    private val reason: String? = null,
) : RadioController {

    private val _state = MutableStateFlow(
        RadioState.unavailable(presets = initialPresets).copy(errorMessage = reason),
    )
    override val state: StateFlow<RadioState> = _state.asStateFlow()

    override fun open() = Unit
    override fun close() = Unit
    override fun setBand(band: RadioBand) = Unit
    override fun tune(frequencyKhz: Int) = Unit
    override fun step(up: Boolean) = Unit
    override fun scan(up: Boolean) = Unit
    override fun cancelScan() = Unit
    override fun setMuted(muted: Boolean) = Unit

    override fun savePreset(slot: Int) = Unit

    override fun loadPreset(slot: Int) = Unit

    override fun clearPreset(slot: Int) {
        val current = _state.value
        val next = current.presets.filter { it.slot != slot }
        _state.value = current.copy(presets = next)
        onPresetsChanged?.invoke(next)
    }
}
