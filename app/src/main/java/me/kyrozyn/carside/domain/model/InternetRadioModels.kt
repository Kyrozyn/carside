package me.kyrozyn.carside.domain.model

data class InternetStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: List<String>,
    val country: String?,
    val bitrate: Int,
    val codec: String?,
    val votes: Int,
    val clickCount: Int,
)

enum class SortOrder { POPULAR, MOST_VOTED, RECENT }

data class NowPlaying(
    val stationName: String,
    val title: String?,
    val artworkUrl: String?,
)

data class InternetRadioState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val query: String = "",
    val tagFilter: String? = null,
    val sort: SortOrder = SortOrder.POPULAR,
    val stations: List<InternetStation> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val favorites: List<InternetStation> = emptyList(),
    val playing: InternetStation? = null,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val nowPlaying: NowPlaying? = null,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
)
