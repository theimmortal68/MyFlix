package dev.jausc.myflix.mobile.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.data.ServerManager
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.launch

/**
 * Dialog for authorizing a Quick Connect code from the TV app.
 *
 * This is shown when the mobile app receives a deep link from scanning
 * a QR code displayed on the TV login screen.
 *
 * @param serverUrl The Jellyfin server URL from the QR code
 * @param code The Quick Connect code to authorize
 * @param serverManager For checking if user is logged into the target server
 * @param jellyfinClient For making the authorization API call
 * @param onDismiss Called when the dialog is dismissed
 * @param onAuthorized Called after successful authorization
 */
@Composable
fun QuickConnectAuthorizationDialog(
    serverUrl: String,
    code: String,
    serverManager: ServerManager,
    jellyfinClient: JellyfinClient,
    onDismiss: () -> Unit,
    onAuthorized: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Find a server matching the URL (normalize for comparison)
    val servers by serverManager.servers.collectAsState()
    val normalizedTargetUrl = serverUrl.trimEnd('/')
    val matchingServer = servers.find {
        it.serverUrl.trimEnd('/').equals(normalizedTargetUrl, ignoreCase = true)
    }

    var isAuthorizing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isAuthorizing) onDismiss() },
        title = {
            Text(
                text = "Authorize Quick Connect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                if (matchingServer != null) {
                    Text(
                        text = "Authorize login to TV for:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = matchingServer.serverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "as ${matchingServer.userName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Code: $code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Not logged into this server",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please log in to this server first in Settings, then scan the QR code again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                error?.let { errorMessage ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            if (matchingServer != null) {
                Button(
                    onClick = {
                        scope.launch {
                            isAuthorizing = true
                            error = null

                            // Configure client for the target server
                            jellyfinClient.configure(
                                matchingServer.serverUrl,
                                matchingServer.accessToken,
                                matchingServer.userId,
                                jellyfinClient.deviceId,
                            )

                            jellyfinClient.authorizeQuickConnect(code)
                                .onSuccess {
                                    Toast.makeText(
                                        context,
                                        "TV authorized successfully!",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    onAuthorized()
                                }
                                .onFailure { e ->
                                    error = when {
                                        e.message?.contains("400") == true -> "Invalid or expired code"
                                        e.message?.contains("401") == true -> "Authorization failed"
                                        else -> e.message ?: "Authorization failed"
                                    }
                                }

                            isAuthorizing = false
                        }
                    },
                    enabled = !isAuthorizing,
                ) {
                    if (isAuthorizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Authorize")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAuthorizing,
            ) {
                Text("Cancel")
            }
        },
    )
}
