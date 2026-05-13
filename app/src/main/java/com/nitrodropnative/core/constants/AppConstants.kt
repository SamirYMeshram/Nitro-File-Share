package com.nitrodropnative.core.constants

object AppConstants {
    const val APP_NAME = "NitroDrop"
    const val APP_TAGLINE = "Native device-to-device file transfer at maximum sustained speed."

    const val DEFAULT_PORT = 8988
    const val WEB_TRANSFER_PORT = 8989
    const val NSD_SERVICE_TYPE = "_nitrodrop._tcp."
    const val NSD_SERVICE_NAME = "NitroDrop Native"

    const val SMALL_CHUNK_SIZE = 1 * 1024 * 1024
    const val DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024
    const val LARGE_CHUNK_SIZE = 8 * 1024 * 1024
    const val SOCKET_BUFFER_SIZE = 8 * 1024 * 1024
    const val WEB_SOCKET_BUFFER_SIZE = 16 * 1024 * 1024
    const val WEB_UPLOAD_CHUNK_SIZE = 16 * 1024 * 1024

    const val SPEED_WINDOW_MS = 2_000L
    const val UI_UPDATE_INTERVAL_MS = 250L
    const val UI_UPDATE_INTERVAL_SLOW_MS = 500L

    const val PROTOCOL_VERSION = 1
    const val PROTOCOL_MAGIC = "NDROP1"
    const val PARTIAL_EXTENSION = ".nitro_part"
    const val DOWNLOADS_FOLDER = "Download/NitroDrop"

    const val NOTIFICATION_CHANNEL_ID = "nitrodrop_transfer"
    const val NOTIFICATION_ID = 4107
}
