@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.screens

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.tv.TvPreferences
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

/**
 * Seerr setup screen with autodiscovery and auto-login.
 *
 * Flow:
 * 1. Try autodiscovery based on Jellyfin server URL
 * 2. If found, auto-login with Jellyfin credentials
 * 3. If not found, show manual URL entry
 * 4. Success confirmation
 */
@Composable
fun SeerrSetupScreen(
    seerrClient: SeerrClient,
    preferences: TvPreferences,
    jellyfinUsername: String? = null,
    jellyfinPassword: String? = null,
    jellyfinServerUrl: String? = null,
    onSetupComplete: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var currentStep by remember {
        mutableIntStateOf(0)
    } // 0 = discovering, 1 = manual/credentials, 2 = logging in, 3 = done
    var serverUrl by remember { mutableStateOf("") }
    var manualUsername by remember { mutableStateOf("") }
    var manualPassword by remember { mutableStateOf("") }
    val defaultAuthMode = if (!jellyfinUsername.isNullOrBlank()) {
        SeerrAuthMode.JELLYFIN
    } else {
        SeerrAuthMode.LOCAL
    }
    var authMode by remember { mutableStateOf(defaultAuthMode) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Searching for Seerr server...") }
    var needsManualCredentials by remember { mutableStateOf(false) }
    var serverConnected by remember { mutableStateOf(false) } // Track if server was connected by autodiscovery

    // Check if credentials are available
    val hasCredentials = !jellyfinUsername.isNullOrBlank() && !jellyfinPassword.isNullOrBlank()

    LaunchedEffect(authMode, hasCredentials) {
        needsManualCredentials = authMode == SeerrAuthMode.LOCAL || !hasCredentials
    }

    // Autodiscovery on launch
    LaunchedEffect(Unit) {
        needsManualCredentials = authMode == SeerrAuthMode.LOCAL || !hasCredentials

        isLoading = true

        // Build list of URLs to try based on Jellyfin server
        val urlsToTry = mutableListOf<String>()

        if (!jellyfinServerUrl.isNullOrBlank()) {
            // Extract base from Jellyfin URL (e.g., http://192.168.1.100:8096 -> http://192.168.1.100)
            val baseUrl = jellyfinServerUrl
                .replace(Regex(":\\d+/?$"), "") // Remove port
                .trimEnd('/')

            // Common Seerr ports and paths
            urlsToTry.addAll(
                listOf(
                    "$baseUrl:5055", // Default Jellyseerr/Overseerr port
                    "$baseUrl:5056", // Alternative port
                    "${baseUrl.replace("http://", "https://")}:5055", // HTTPS variant
                    "$baseUrl/jellyseerr", // Reverse proxy path
                    "$baseUrl/overseerr", // Reverse proxy path
                    "$baseUrl/seerr", // Reverse proxy path
                ),
            )
        }

        // Also try localhost variants
        urlsToTry.addAll(
            listOf(
                "http://localhost:5055",
                "http://127.0.0.1:5055",
            ),
        )

        var foundUrl: String? = null

        for (url in urlsToTry) {
            statusMessage = "Trying $url..."

            val result = seerrClient.connectToServer(url)
            if (result.isSuccess) {
                foundUrl = url
                break
            }
        }

        if (foundUrl != null) {
            serverUrl = foundUrl
            serverConnected = true // Mark that we connected via autodiscovery
            statusMessage = "Found Seerr at $foundUrl"

            // Auto-login with Jellyfin credentials if available
            if (hasCredentials && authMode == SeerrAuthMode.JELLYFIN) {
                statusMessage = "Logging in as $jellyfinUsername..."
                currentStep = 2

                seerrClient.loginWithJellyfin(jellyfinUsername.orEmpty(), jellyfinPassword.orEmpty())
                    .onSuccess { user ->
                        preferences.setSeerrUrl(foundUrl)
                        preferences.setSeerrEnabled(true)
                        preferences.setSeerrAutoDetected(true)
                        user.apiKey?.let { preferences.setSeerrApiKey(it) }
                        seerrClient.sessionCookie?.let { preferences.setSeerrSessionCookie(it) }
                        currentStep = 3
                    }
                    .onFailure {
                        errorMessage = "Auto-login failed: ${it.message}"
                        currentStep = 1 // Fall back to manual
                    }
            } else {
                // Need manual credentials entry
                statusMessage = "Found Seerr - enter credentials to login"
                currentStep = 1
            }
        } else {
            statusMessage = "Could not find Seerr server automatically"
            currentStep = 1 // Manual entry
        }

        isLoading = false
    }

    // Get effective credentials (manual or stored)
    fun getEffectiveCredentials(): Pair<String, String>? {
        return if (needsManualCredentials) {
            if (manualUsername.isNotBlank() && manualPassword.isNotBlank()) {
                Pair(manualUsername, manualPassword)
            } else {
                null
            }
        } else if (hasCredentials) {
            Pair(jellyfinUsername.orEmpty(), jellyfinPassword.orEmpty())
        } else {
            null
        }
    }

    // Manual connection and login
    fun connectAndLogin() {
        val creds = getEffectiveCredentials()
        if (creds == null) {
            errorMessage = "Please enter username and password"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            var url = serverUrl.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            url = url.trimEnd('/')

            seerrClient.connectToServer(url)
                .onSuccess {
                    serverUrl = url
                    currentStep = 2

                    val authResult = if (authMode == SeerrAuthMode.LOCAL) {
                        seerrClient.loginWithLocal(creds.first, creds.second)
                    } else {
                        seerrClient.loginWithJellyfin(creds.first, creds.second)
                    }
                    authResult
                        .onSuccess { user ->
                            preferences.setSeerrUrl(url)
                            preferences.setSeerrEnabled(true)
                            preferences.setSeerrAutoDetected(false)
                            user.apiKey?.let { preferences.setSeerrApiKey(it) }
                            seerrClient.sessionCookie?.let { preferences.setSeerrSessionCookie(it) }
                            currentStep = 3
                        }
                        .onFailure {
                            errorMessage = "Login failed: ${it.message}"
                            currentStep = 1
                            isLoading = false
                        }
                }
                .onFailure {
                    errorMessage = "Could not connect: ${it.message}"
                    isLoading = false
                }
        }
    }

    // Login only (when server already connected)
    fun loginOnly() {
        val creds = getEffectiveCredentials()
        if (creds == null) {
            errorMessage = "Please enter username and password"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            currentStep = 2

            seerrClient.loginWithJellyfin(creds.first, creds.second)
                .onSuccess { user ->
                    preferences.setSeerrUrl(serverUrl)
                    preferences.setSeerrEnabled(true)
                    preferences.setSeerrAutoDetected(serverUrl.isNotBlank())
                    user.apiKey?.let { preferences.setSeerrApiKey(it) }
                    seerrClient.sessionCookie?.let { preferences.setSeerrSessionCookie(it) }
                    currentStep = 3
                }
                .onFailure {
                    errorMessage = "Login failed: ${it.message}"
                    currentStep = 1
                    isLoading = false
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.Surface.copy(alpha = 0.7f),
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF8B5CF6),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Seerr Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when (currentStep) {
                    0 -> {
                        // Autodiscovery in progress
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(400.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF8B5CF6),
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Looking for Seerr",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TvColors.TextPrimary,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            TvLoadingIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF8B5CF6),
                                strokeWidth = 3.dp,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                            )
                        }
                    }

                    1 -> {
                        // Manual entry - URL and/or credentials
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(500.dp),
                        ) {
                            // Title based on what's needed
                            Text(
                                text = if (serverUrl.isNotBlank() && needsManualCredentials) {
                                    "Enter Jellyfin Credentials"
                                } else {
                                    "Seerr Setup"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TvColors.TextPrimary,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (serverUrl.isNotBlank() && needsManualCredentials) {
                                    val authLabel = if (authMode == SeerrAuthMode.LOCAL) "local" else "Jellyfin"
                                    "Found Seerr at $serverUrl\nEnter your $authLabel credentials to login."
                                } else if (needsManualCredentials) {
                                    val authLabel = if (authMode == SeerrAuthMode.LOCAL) "local" else "Jellyfin"
                                    "Enter server URL and $authLabel credentials"
                                } else {
                                    "Could not auto-detect. Enter the address manually."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Server URL field (if not already found)
                            if (serverUrl.isBlank() || !needsManualCredentials) {
                                SeerrEditText(
                                    label = "Server URL",
                                    hint = "http://192.168.1.100:5055",
                                    value = serverUrl,
                                    onValueChange = { serverUrl = it },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Credentials fields (if needed)
                            if (needsManualCredentials) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SeerrAuthMode.entries.forEach { mode ->
                                        val isSelected = authMode == mode
                                        Button(
                                            onClick = { authMode = mode },
                                            modifier = Modifier.height(20.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            colors = if (isSelected) {
                                                ButtonDefaults.colors(
                                                    containerColor = TvColors.BluePrimary,
                                                    contentColor = TvColors.TextPrimary,
                                                    focusedContainerColor = TvColors.BluePrimary,
                                                )
                                            } else {
                                                ButtonDefaults.colors(
                                                    containerColor = TvColors.Surface,
                                                    contentColor = TvColors.TextPrimary,
                                                    focusedContainerColor = TvColors.FocusedSurface,
                                                )
                                            },
                                        ) {
                                            Text(mode.label, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                SeerrEditText(
                                    label = if (authMode == SeerrAuthMode.LOCAL) "Email" else "Jellyfin Username",
                                    hint = "username",
                                    value = manualUsername,
                                    onValueChange = { manualUsername = it },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                SeerrEditText(
                                    label = if (authMode == SeerrAuthMode.LOCAL) "Password" else "Jellyfin Password",
                                    hint = "password",
                                    value = manualPassword,
                                    onValueChange = { manualPassword = it },
                                    enabled = !isLoading,
                                    isPassword = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // Login/Connect button
                            val canProceed = if (needsManualCredentials) {
                                manualUsername.isNotBlank() && manualPassword.isNotBlank() && serverUrl.isNotBlank()
                            } else {
                                serverUrl.isNotBlank()
                            }

                            Button(
                                onClick = {
                                    // Only use loginOnly if server was connected by autodiscovery
                                    if (serverConnected && needsManualCredentials) {
                                        loginOnly()
                                    } else {
                                        connectAndLogin()
                                    }
                                },
                                enabled = canProceed && !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.colors(containerColor = Color(0xFF8B5CF6)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    if (isLoading) {
                                        TvLoadingIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = TvColors.TextPrimary,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Connecting...")
                                    } else {
                                        Text(
                                            text = if (serverUrl.isNotBlank() && needsManualCredentials) "Login" else "Connect",
                                        )
                                    }
                                }
                            }

                            // Error message
                            errorMessage?.let { error ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(TvColors.Error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Error,
                                        contentDescription = null,
                                        tint = TvColors.Error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TvColors.Error,
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // Logging in
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(400.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(48.dp),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Connected to Seerr",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TvColors.TextPrimary,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = serverUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            TvLoadingIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF8B5CF6),
                                strokeWidth = 3.dp,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Logging in as ${if (needsManualCredentials) manualUsername else jellyfinUsername}...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                            )
                        }
                    }

                    3 -> {
                        // Success
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(400.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFF22C55E).copy(alpha = 0.1f), RoundedCornerShape(40.dp))
                                    .padding(16.dp),
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Setup Complete!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TvColors.TextPrimary,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Seerr is ready to use.\nDiscover and request new content.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = onSetupComplete,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.colors(containerColor = Color(0xFF8B5CF6)),
                            ) {
                                Text("Get Started")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrEditText(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AndroidView(
            factory = { ctx ->
                EditText(ctx).apply {
                    this.hint = hint
                    inputType = if (isPassword) {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                    }
                    if (isPassword) {
                        transformationMethod = PasswordTransformationMethod.getInstance()
                    }
                    isSingleLine = true
                    setTextColor(TvColors.TextPrimary.toArgb())
                    setHintTextColor(TvColors.TextSecondary.toArgb())
                    setBackgroundColor(TvColors.Surface.toArgb())
                    setPadding(32, 24, 32, 24)
                    textSize = 18f
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setText(value)
                    doAfterTextChanged { onValueChange(it?.toString() ?: "") }
                }
            },
            update = { editText ->
                editText.isEnabled = enabled
                if (editText.text.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
    }
}

private enum class SeerrAuthMode(val label: String) {
    JELLYFIN("Jellyfin"),
    LOCAL("Local"),
}
