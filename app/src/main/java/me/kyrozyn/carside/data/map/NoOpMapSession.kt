package me.kyrozyn.carside.data.map

import me.kyrozyn.carside.domain.repository.MapSession

class NoOpMapSession : MapSession {
    override fun onResume() = Unit
    override fun onPause() = Unit
}
