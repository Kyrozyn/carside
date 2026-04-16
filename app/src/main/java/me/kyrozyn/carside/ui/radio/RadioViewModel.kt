package me.kyrozyn.carside.ui.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.RadioBand
import me.kyrozyn.carside.domain.model.RadioState
import me.kyrozyn.carside.domain.repository.RadioController

class RadioViewModel(
    private val controller: RadioController,
) : ViewModel() {

    val uiState: StateFlow<RadioState> = controller.state

    init {
        controller.open()
    }

    fun setBand(band: RadioBand) = controller.setBand(band)
    fun tune(frequencyKhz: Int) = controller.tune(frequencyKhz)
    fun stepUp() = controller.step(up = true)
    fun stepDown() = controller.step(up = false)
    fun scanUp() = controller.scan(up = true)
    fun scanDown() = controller.scan(up = false)
    fun cancelScan() = controller.cancelScan()
    fun toggleMute() = controller.setMuted(!uiState.value.isMuted)
    fun savePreset(slot: Int) = controller.savePreset(slot)
    fun loadPreset(slot: Int) = controller.loadPreset(slot)
    fun clearPreset(slot: Int) = controller.clearPreset(slot)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(RadioViewModel::class.java))
                    return RadioViewModel(container.radioController) as T
                }
            }
    }
}
