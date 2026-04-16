package me.kyrozyn.carside.ui.internetradio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.InternetRadioState
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.domain.model.SortOrder
import me.kyrozyn.carside.domain.repository.InternetRadioController

class InternetRadioViewModel(
    private val controller: InternetRadioController,
) : ViewModel() {

    val uiState: StateFlow<InternetRadioState> = controller.state

    init {
        controller.refresh()
    }

    fun refresh() = controller.refresh()
    fun setQuery(query: String) = controller.setQuery(query)
    fun setTag(tag: String?) = controller.setTag(tag)
    fun setSort(sort: SortOrder) = controller.setSort(sort)
    fun play(station: InternetStation) = controller.play(station)
    fun stop() = controller.stop()
    fun setVolume(volume: Float) = controller.setVolume(volume)
    fun toggleMute() = controller.toggleMute()

    fun toggleFavorite(station: InternetStation) {
        val isFav = uiState.value.favorites.any { it.uuid == station.uuid }
        if (isFav) controller.removeFavorite(station.uuid)
        else controller.addFavorite(station)
    }

    override fun onCleared() {
        // NOTE: jangan panggil stop/release di sini — controller hidup di AppContainer
        // agar playback tetap berjalan saat user kembali ke Dashboard (lihat mini panel).
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(InternetRadioViewModel::class.java))
                    return InternetRadioViewModel(container.internetRadioController) as T
                }
            }
    }
}
