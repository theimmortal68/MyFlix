package dev.jausc.myflix.core.network.websocket

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Parses WebSocket messages from the Jellyfin server into typed events.
 */
class WebSocketEventParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /**
     * Parse a JSON message string into a WebSocketEvent.
     * Returns null for unknown or malformed messages.
     */
    fun parse(message: String): WebSocketEvent? {
        return try {
            val element = json.parseToJsonElement(message).jsonObject
            val messageType = element["MessageType"]?.jsonPrimitive?.contentOrNull

            // Handle KeepAlive early - Data is a number (timeout), not an object
            if (messageType == "ForceKeepAlive" || messageType == "KeepAlive") {
                return WebSocketEvent.KeepAlive
            }

            // For other message types, Data is a JsonObject
            val data = element["Data"]?.jsonObject

            when (messageType) {
                "Playstate" -> parsePlaystateMessage(data)
                "Play" -> parsePlayMessage(data)
                "GeneralCommand" -> parseGeneralCommand(data)
                "LibraryChanged" -> parseLibraryChanged(data)
                "UserDataChanged" -> parseUserDataChanged(data)
                else -> {
                    Log.d(TAG, "Ignoring unknown message type: $messageType")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse WebSocket message: ${e.message}")
            null
        }
    }

    private fun parsePlaystateMessage(data: JsonObject?): WebSocketEvent.PlaystateCommand? {
        if (data == null) return null

        val commandStr = data["Command"]?.jsonPrimitive?.contentOrNull ?: return null
        val command = PlaystateCommandType.fromString(commandStr) ?: return null

        return WebSocketEvent.PlaystateCommand(
            command = command,
            seekPositionTicks = data["SeekPositionTicks"]?.jsonPrimitive?.longOrNull,
            controllingUserId = data["ControllingUserId"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parsePlayMessage(data: JsonObject?): WebSocketEvent.PlayCommand? {
        if (data == null) return null

        val itemIds = data["ItemIds"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: return null

        val playCommandStr = data["PlayCommand"]?.jsonPrimitive?.contentOrNull ?: return null
        val playCommand = PlayCommandType.fromString(playCommandStr) ?: return null

        return WebSocketEvent.PlayCommand(
            itemIds = itemIds,
            startPositionTicks = data["StartPositionTicks"]?.jsonPrimitive?.longOrNull,
            playCommand = playCommand,
            startIndex = data["StartIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            audioStreamIndex = data["AudioStreamIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            subtitleStreamIndex = data["SubtitleStreamIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            mediaSourceId = data["MediaSourceId"]?.jsonPrimitive?.contentOrNull,
            controllingUserId = data["ControllingUserId"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseGeneralCommand(data: JsonObject?): WebSocketEvent.GeneralCommand? {
        if (data == null) return null

        val nameStr = data["Name"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = GeneralCommandType.fromString(nameStr)
        if (name == null) {
            Log.d(TAG, "Unknown general command: $nameStr")
            return null
        }

        val arguments = data["Arguments"]?.jsonObject
            ?.mapValues { it.value.jsonPrimitive.contentOrNull }
            ?: emptyMap()

        return WebSocketEvent.GeneralCommand(
            name = name,
            arguments = arguments,
            controllingUserId = data["ControllingUserId"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseLibraryChanged(data: JsonObject?): WebSocketEvent.LibraryChanged {
        return WebSocketEvent.LibraryChanged(
            itemsAdded = data?.get("ItemsAdded")?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList(),
            itemsRemoved = data?.get("ItemsRemoved")?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList(),
            itemsUpdated = data?.get("ItemsUpdated")?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList(),
        )
    }

    private fun parseUserDataChanged(data: JsonObject?): WebSocketEvent.UserDataChanged? {
        if (data == null) return null

        val userId = data["UserId"]?.jsonPrimitive?.contentOrNull ?: return null
        val userDataList = data["UserDataList"]?.jsonArray?.firstOrNull()?.jsonObject
        val itemId = userDataList?.get("ItemId")?.jsonPrimitive?.contentOrNull ?: return null

        return WebSocketEvent.UserDataChanged(
            userId = userId,
            itemId = itemId,
        )
    }

    companion object {
        private const val TAG = "WebSocketEventParser"
    }
}
