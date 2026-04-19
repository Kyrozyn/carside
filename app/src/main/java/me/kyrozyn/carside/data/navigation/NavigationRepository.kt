package me.kyrozyn.carside.data.navigation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.kyrozyn.carside.domain.model.NavSource
import me.kyrozyn.carside.domain.model.NavigationState
import me.kyrozyn.carside.media.MediaSessionBridge
import me.kyrozyn.carside.media.NavigationNotificationHints

/**
 * Meng-expose snapshot navigasi (Google Maps / Waze) sebagai [StateFlow] untuk dikonsumsi UI.
 *
 * Sumber data: [NavigationNotificationHints] yang diisi oleh
 * [me.kyrozyn.carside.media.CarsideNotificationListenerService]. Sinkronisasi push dilakukan
 * lewat [MediaSessionBridge.onNavigationHintUpdated]; [refresh] bisa dipanggil manual dari
 * activity onResume untuk memastikan state terbaru setelah app kembali ke foreground.
 */
class NavigationRepository(context: Context) {

    @Suppress("unused") private val appContext = context.applicationContext

    private val _state = MutableStateFlow(NavigationState.Idle)
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    init {
        MediaSessionBridge.onNavigationHintUpdated = { refresh() }
        refresh()
    }

    fun refresh() {
        _state.value = buildState()
    }

    private fun buildState(): NavigationState {
        val hint = NavigationNotificationHints.get() ?: return NavigationState.Idle
        val source = NavSource.fromPackage(hint.packageName)
        return NavigationState(
            active = true,
            sourceApp = source,
            maneuverText = hint.maneuverText,
            roadText = hint.roadText,
            distanceText = hint.distanceText,
            etaText = hint.etaText,
            iconUri = hint.iconUri,
        )
    }
}
