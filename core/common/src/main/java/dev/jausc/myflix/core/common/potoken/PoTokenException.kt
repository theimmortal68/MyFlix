package dev.jausc.myflix.core.common.potoken

/**
 * Exception thrown when poToken generation fails.
 */
class PoTokenException(message: String) : Exception(message)

/**
 * Exception thrown when the WebView implementation is broken (e.g., too old JavaScript support).
 */
class BadWebViewException(message: String) : Exception(message)

/**
 * Builds the appropriate exception type based on the JavaScript error message.
 */
fun buildExceptionForJsError(error: String): Exception {
    return if (error.contains("SyntaxError")) {
        BadWebViewException(error)
    } else {
        PoTokenException(error)
    }
}
