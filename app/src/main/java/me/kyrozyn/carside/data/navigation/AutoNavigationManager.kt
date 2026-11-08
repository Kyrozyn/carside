package me.kyrozyn.carside.data.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.kyrozyn.carside.core.AutoPlaceResult
import me.kyrozyn.carside.core.AutoRouteResult

data class AutoNavState(
    val isNavigating: Boolean = false,
    val selectedPlace: AutoPlaceResult? = null,
    val availableRoutes: List<AutoRouteResult> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val currentStepIndex: Int = 0,
    val passedPointIndex: Int = 0,
) {
    val activeRoute: AutoRouteResult?
        get() = availableRoutes.getOrNull(selectedRouteIndex)
}

/**
 * Singleton manager untuk menyimpan sesi navigasi Mode Auto (Google Maps),
 * sehingga state navigasi tetap aktif saat berpindah ke pengaturan, drawer, atau aplikasi lain.
 */
class AutoNavigationManager {

    private val _navState = MutableStateFlow(AutoNavState())
    val navState: StateFlow<AutoNavState> = _navState.asStateFlow()

    fun setPreviewRoutes(place: AutoPlaceResult?, routes: List<AutoRouteResult>) {
        _navState.update {
            it.copy(
                selectedPlace = place,
                availableRoutes = routes,
                selectedRouteIndex = 0,
                currentStepIndex = 0,
                passedPointIndex = 0,
                isNavigating = false,
            )
        }
    }

    fun selectRoute(index: Int) {
        _navState.update {
            if (index in it.availableRoutes.indices) {
                it.copy(selectedRouteIndex = index, currentStepIndex = 0, passedPointIndex = 0)
            } else it
        }
    }

    fun startNavigation() {
        _navState.update {
            if (it.availableRoutes.isNotEmpty()) {
                it.copy(isNavigating = true, currentStepIndex = 0, passedPointIndex = 0)
            } else it
        }
    }

    fun stopNavigation() {
        _navState.update {
            AutoNavState()
        }
    }

    fun nextStep() {
        _navState.update { state ->
            val steps = state.activeRoute?.steps.orEmpty()
            if (state.currentStepIndex < steps.size - 1) {
                val nextIdx = state.currentStepIndex + 1
                state.copy(
                    currentStepIndex = nextIdx,
                    passedPointIndex = steps[nextIdx].startPointIndex,
                )
            } else state
        }
    }

    fun previousStep() {
        _navState.update { state ->
            val steps = state.activeRoute?.steps.orEmpty()
            if (state.currentStepIndex > 0) {
                val prevIdx = state.currentStepIndex - 1
                state.copy(
                    currentStepIndex = prevIdx,
                    passedPointIndex = steps.getOrNull(prevIdx)?.startPointIndex ?: 0,
                )
            } else state
        }
    }

    fun setStepIndex(index: Int) {
        _navState.update { state ->
            val maxSteps = state.activeRoute?.steps?.size ?: 0
            if (index in 0 until maxSteps) {
                state.copy(currentStepIndex = index)
            } else state
        }
    }

    fun updateProgress(passedPointIndex: Int, stepIndex: Int) {
        _navState.update { state ->
            val maxSteps = state.activeRoute?.steps?.size ?: 0
            val validStep = if (maxSteps > 0) stepIndex.coerceIn(0, maxSteps - 1) else 0
            if (state.passedPointIndex == passedPointIndex && state.currentStepIndex == validStep) {
                state
            } else {
                state.copy(
                    passedPointIndex = passedPointIndex,
                    currentStepIndex = validStep,
                )
            }
        }
    }

    fun updateReroutedNavigation(routes: List<AutoRouteResult>) {
        if (routes.isEmpty()) return
        _navState.update { state ->
            if (!state.isNavigating) state
            else state.copy(
                availableRoutes = routes,
                selectedRouteIndex = 0,
                currentStepIndex = 0,
                passedPointIndex = 0,
            )
        }
    }
}
