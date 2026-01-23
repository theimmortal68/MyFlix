package dev.jausc.myflix.core.common.potoken

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the raw challenge data obtained from the Create endpoint and returns
 * an object that can be embedded in a JavaScript snippet.
 */
fun parseChallengeData(rawChallengeData: String): String {
    val scrambled = JSONArray(rawChallengeData)

    val challengeData = if (scrambled.length() > 1 && !scrambled.isNull(1) &&
        scrambled.optString(1).isNotEmpty()
    ) {
        val descrambled = descramble(scrambled.getString(1))
        JSONArray(descrambled)
    } else {
        scrambled.getJSONArray(0)
    }

    val messageId = challengeData.getString(0)
    val interpreterHash = challengeData.getString(3)
    val program = challengeData.getString(4)
    val globalName = challengeData.getString(5)
    val clientExperimentsStateBlob = challengeData.getString(7)

    val interpreterArray1 = challengeData.optJSONArray(1)
    val interpreterArray2 = challengeData.optJSONArray(2)

    val privateDoNotAccessOrElseSafeScriptWrappedValue = interpreterArray1?.let { arr ->
        (0 until arr.length()).firstNotNullOfOrNull { i ->
            arr.optString(i).takeIf { it.isNotEmpty() }
        }
    }

    val privateDoNotAccessOrElseTrustedResourceUrlWrappedValue = interpreterArray2?.let { arr ->
        (0 until arr.length()).firstNotNullOfOrNull { i ->
            arr.optString(i).takeIf { it.isNotEmpty() }
        }
    }

    return JSONObject().apply {
        put("messageId", messageId)
        put("interpreterJavascript", JSONObject().apply {
            put(
                "privateDoNotAccessOrElseSafeScriptWrappedValue",
                privateDoNotAccessOrElseSafeScriptWrappedValue
            )
            put(
                "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue",
                privateDoNotAccessOrElseTrustedResourceUrlWrappedValue
            )
        })
        put("interpreterHash", interpreterHash)
        put("program", program)
        put("globalName", globalName)
        put("clientExperimentsStateBlob", clientExperimentsStateBlob)
    }.toString()
}

/**
 * Parses the raw integrity token data obtained from the GenerateIT endpoint.
 * @return A pair of the JavaScript Uint8Array representation and the token duration in seconds
 */
fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
    val integrityTokenData = JSONArray(rawIntegrityTokenData)
    return base64ToU8(integrityTokenData.getString(0)) to integrityTokenData.getLong(1)
}

/**
 * Converts a string (usually the identifier used as input to obtainPoToken) to a JavaScript
 * Uint8Array that can be embedded directly in JavaScript code.
 */
fun stringToU8(identifier: String): String = newUint8Array(identifier.toByteArray())

/**
 * Takes a poToken encoded as a sequence of bytes represented as integers separated by commas
 * (e.g. "97,98,99" would be "abc"), which is the output of Uint8Array::toString() in JavaScript,
 * and converts it to the specific base64 representation for poTokens.
 */
fun u8ToBase64(poToken: String): String {
    val bytes = poToken.split(",")
        .map { it.toInt().toByte() }
        .toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
        .replace("+", "-")
        .replace("/", "_")
}

/**
 * Takes the scrambled challenge, decodes it from base64, adds 97 to each byte.
 */
private fun descramble(scrambledChallenge: String): String {
    val bytes = base64ToByteArray(scrambledChallenge)
    return bytes.map { ((it.toInt() and 0xFF) + 97).toByte() }
        .toByteArray()
        .decodeToString()
}

/**
 * Decodes a base64 string encoded in the specific base64 representation used by YouTube, and
 * returns a JavaScript Uint8Array that can be embedded directly in JavaScript code.
 */
private fun base64ToU8(base64: String): String = newUint8Array(base64ToByteArray(base64))

private fun newUint8Array(contents: ByteArray): String =
    "new Uint8Array([" + contents.joinToString(",") { (it.toInt() and 0xFF).toString() } + "])"

/**
 * Decodes a base64 string encoded in the specific base64 representation used by YouTube.
 */
private fun base64ToByteArray(base64: String): ByteArray {
    val base64Mod = base64
        .replace('-', '+')
        .replace('_', '/')
        .replace('.', '=')

    return Base64.decode(base64Mod, Base64.DEFAULT)
        ?: throw PoTokenException("Cannot base64 decode")
}
