package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.Flow
import me.kyrozyn.carside.domain.model.CompassHeading

interface CompassProvider {
    val heading: Flow<CompassHeading>
}
