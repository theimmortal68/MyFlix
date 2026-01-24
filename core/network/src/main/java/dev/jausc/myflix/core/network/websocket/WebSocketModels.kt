package dev.jausc.myflix.core.network.websocket

/**
 * WebSocket connection state.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Events received from the Jellyfin server via WebSocket.
 */
sealed class WebSocketEvent {
    /**
     * Playback state command (pause, seek, stop, etc.)
     */
    data class PlaystateCommand(
        val command: PlaystateCommandType,
        val seekPositionTicks: Long? = null,
        val controllingUserId: String? = null,
    ) : WebSocketEvent()

    /**
     * Command to play specific items.
     */
    data class PlayCommand(
        val itemIds: List<String>,
        val startPositionTicks: Long? = null,
        val playCommand: PlayCommandType,
        val startIndex: Int? = null,
        val audioStreamIndex: Int? = null,
        val subtitleStreamIndex: Int? = null,
        val mediaSourceId: String? = null,
        val controllingUserId: String? = null,
    ) : WebSocketEvent()

    /**
     * General command (navigation, display message, volume, etc.)
     */
    data class GeneralCommand(
        val name: GeneralCommandType,
        val arguments: Map<String, String?>,
        val controllingUserId: String? = null,
    ) : WebSocketEvent()

    /**
     * Server keep-alive ping - must respond with KeepAlive message.
     */
    data object KeepAlive : WebSocketEvent()

    /**
     * Library content changed notification.
     */
    data class LibraryChanged(
        val itemsAdded: List<String>,
        val itemsRemoved: List<String>,
        val itemsUpdated: List<String>,
    ) : WebSocketEvent()

    /**
     * User data changed (favorites, watched status, etc.)
     */
    data class UserDataChanged(
        val userId: String,
        val itemId: String,
    ) : WebSocketEvent()
}

/**
 * Playback state command types.
 */
enum class PlaystateCommandType {
    Stop,
    Pause,
    Unpause,
    NextTrack,
    PreviousTrack,
    Seek,
    Rewind,
    FastForward,
    PlayPause,
    ;

    companion object {
        fun fromString(value: String): PlaystateCommandType? = entries.find { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * Play command types.
 */
enum class PlayCommandType {
    PlayNow,
    PlayNext,
    PlayLast,
    PlayInstantMix,
    PlayShuffle,
    ;

    companion object {
        fun fromString(value: String): PlayCommandType? = entries.find { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * General command types supported by the client.
 * These match the commands registered in JellyfinClient.registerSessionCapabilities().
 */
enum class GeneralCommandType {
    // Navigation
    MoveUp,
    MoveDown,
    MoveLeft,
    MoveRight,
    PageUp,
    PageDown,
    PreviousLetter,
    NextLetter,
    Select,
    Back,

    // Playback control
    PlayState,
    PlayNext,
    Play,
    PlayMediaSource,
    PlayTrailers,
    SetRepeatMode,
    SetShuffleQueue,
    SetPlaybackOrder,

    // Volume
    Mute,
    Unmute,
    ToggleMute,
    SetVolume,
    VolumeUp,
    VolumeDown,

    // Stream selection
    SetAudioStreamIndex,
    SetSubtitleStreamIndex,
    SetMaxStreamingBitrate,

    // UI
    ToggleOsd,
    ToggleOsdMenu,
    ToggleContextMenu,
    ToggleFullscreen,
    DisplayContent,
    DisplayMessage,
    GoHome,
    GoToSettings,
    GoToSearch,

    // Misc
    SendKey,
    SendString,
    ChannelUp,
    ChannelDown,
    Guide,
    ToggleStats,
    ;

    companion object {
        fun fromString(value: String): GeneralCommandType? = entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
