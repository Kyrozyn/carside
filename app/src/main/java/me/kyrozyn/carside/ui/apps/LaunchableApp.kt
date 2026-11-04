package me.kyrozyn.carside.ui.apps

data class LaunchableApp(
    val packageName: String,
    val activityName: String? = null,
    val label: String,
    val isCarSystemApp: Boolean = false,
) {
    /** Target ID unik untuk launcher & shortcut picker (misal: "com.ts.bt/com.ts.bt.BtMusicActivity" atau "com.spotify.music") */
    val targetId: String
        get() = if (!activityName.isNullOrBlank()) "$packageName/$activityName" else packageName
}
