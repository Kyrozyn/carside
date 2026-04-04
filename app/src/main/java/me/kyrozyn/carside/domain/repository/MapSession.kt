package me.kyrozyn.carside.domain.repository

/**
 * Hook for future map camera / lifecycle coordination. Placeholder for clean architecture.
 */
interface MapSession {
    fun onResume()
    fun onPause()
}
