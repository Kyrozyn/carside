package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.Flow
import me.kyrozyn.carside.domain.model.GeoLocation

interface LocationProvider {
    val locationUpdates: Flow<GeoLocation?>
}
