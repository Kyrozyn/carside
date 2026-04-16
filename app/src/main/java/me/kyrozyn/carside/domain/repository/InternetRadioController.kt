package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.StateFlow
import me.kyrozyn.carside.domain.model.InternetRadioState
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.domain.model.SortOrder

interface InternetRadioController {
    val state: StateFlow<InternetRadioState>

    fun refresh()

    fun setQuery(query: String)
    fun setTag(tag: String?)
    fun setSort(sort: SortOrder)

    fun play(station: InternetStation)
    fun stop()

    fun setVolume(volume: Float)
    fun toggleMute()

    fun addFavorite(station: InternetStation)
    fun removeFavorite(uuid: String)

    fun release()
}
