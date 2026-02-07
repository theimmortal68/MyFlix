@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
    "ModifierMissing",
    "ParameterNaming",
    "ComposableParamOrder",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.ui.SeerrAuthMode
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.mobile.MobilePreferences
import kotlinx.coroutines.launch

/**
 * Mobile Seerr setup wizard.
 *
 * Steps:
 * 1. Connect - Enter server URL or auto-detect
 * 2. Authenticate - Use Jellyfin credentials
 * 3. Done - Success confirmation
 */
@Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun SeerrSetupScreen(
    seerrClient: SeerrClient,
    preferences: MobilePreferences,
    jellyfinUsername: String?,
    jellyfinPassword: String?,
    jellyfinHost: String? = null,
    onSetupComplete: () -> Unit,
    onBack: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var serverUrl by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var isAutoDetecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    val defaultAuthMode = if (!jellyfinUsername.isNullOrBlank()) {
        SeerrAuthMode.JELLYFIN
    } else {
        SeerrAuthMode.LOCAL
    }
    var authMode by remember { mutableStateOf(defaultAuthMode) }
    var username by remember { mutableStateOf(jellyfinUsername ?: "") }
    var password by remember { mutableStateOf(jellyfinPassword ?: "") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Auto-detect on launch if we have jellyfin host
    LaunchedEffect(jellyfinHost) {
        if (!jellyfinHost.isNullOrBlank()) {
            isAutoDetecting = true
            seerrClient.detectServer(jellyfinHost)
                .onSuccess { url ->
                    serverUrl = url
                    seerrClient.connectToServer(url)

                    // Try auto-login if we have credentials
                    if (authMode == SeerrAuthMode.JELLYFIN &&
                        !jellyfinUsername.isNullOrBlank() &&
                        !jellyfinPassword.isNullOrBlank()
                    ) {
                        seerrClient.loginWithJellyfin(jellyfinUsername, jellyfinPassword, jellyfinHost)
                            .onSuccess { user ->
                                preferences.setSeerrEnabled(true)
                                preferences.setSeerrUrl(url)
                                preferences.setSeerrAutoDetected(true)
                                // Save credentials for persistent auth
                                seerrClient.sessionCookie?.let { preferences.setSeerrSessionCookie(it) }
                                currentStep = 3
                            }
                            .onFailure {
                                // Auto-login failed, go to step 2
                                currentStep = 2
                            }
                    } else {
                        currentStep = 2
                    }
                }
            isAutoDetecting = false
        }
    }

    fun connectToServer() {
        if (serverUrl.isBlank()) return
        scope.launch {
            isConnecting = true
            connectionError = null

            var url = serverUrl.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            url = url.trimEnd('/')

            seerrClient.connectToServer(url)
                .onSuccess {
                    serverUrl = url
                    preferences.setSeerrUrl(url)
                    currentStep = 2
                }
                .onFailure {
                    connectionError = it.message ?: "Connection failed"
                }
            isConnecting = false
        }
    }

    fun authenticateWithCredentials() {
        scope.launch {
            isAuthenticating = true
            authError = null

            val authResult = if (authMode == SeerrAuthMode.LOCAL) {
                seerrClient.loginWithLocal(username, password)
            } else {
                seerrClient.loginWithJellyfin(username, password, jellyfinHost)
            }
            authResult
                .onSuccess { user ->
                    preferences.setSeerrUrl(serverUrl) // Save URL (may have been set by auto-detection)
                    preferences.setSeerrEnabled(true)
                    // Save session cookie for persistent auth
                    seerrClient.sessionCookie?.let { preferences.setSeerrSessionCookie(it) }
                    currentStep = 3
                }
                .onFailure {
                    authError = it.message ?: "Authentication failed"
                }
            isAuthenticating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Connect to Seerr",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress indicator
        MobileSeerrSetupProgress(currentStep = currentStep)

        Spacer(modifier = Modifier.height(32.dp))

        // Content based on step
        when (currentStep) {
            1 -> {
                // Step 1: Connect
                MobileSeerrSetupStep(
                    icon = Icons.Outlined.Cloud,
                    title = "Server Connection",
                    description = if (isAutoDetecting) {
                        "Searching for Seerr on your network..."
                    } else {
                        "Enter your Jellyseerr/Overseerr server URL"
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isAutoDetecting) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                    }
                } else {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://192.168.1.100:5055") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { connectToServer() },
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            cursorColor = Color(0xFF8B5CF6),
                        ),
                    )

                    connectionError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { connectToServer() },
                        enabled = serverUrl.isNotBlank() && !isConnecting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                        ),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isConnecting) "Connecting..." else "Connect")
                    }

                    // Auto-detect button
                    if (!jellyfinHost.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isAutoDetecting = true
                                    connectionError = null
                                    seerrClient.detectServer(jellyfinHost)
                                        .onSuccess { url ->
                                            serverUrl = url
                                            seerrClient.connectToServer(url)
                                            preferences.setSeerrUrl(url)
                                            currentStep = 2
                                        }
                                        .onFailure {
                                            connectionError = "Could not auto-detect Seerr"
                                        }
                                    isAutoDetecting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Detect")
                        }
                    }
                }
            }

            2 -> {
                // Step 2: Authenticate
                MobileSeerrSetupStep(
                    icon = Icons.Outlined.Person,
                    title = "Authentication",
                    description = if (authMode == SeerrAuthMode.JELLYFIN) {
                        "Sign in with your Jellyfin credentials"
                    } else {
                        "Sign in with your Seerr account"
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeerrAuthMode.entries.forEach { mode ->
                        FilterChip(
                            selected = authMode == mode,
                            onClick = { authMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF8B5CF6),
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (authMode == SeerrAuthMode.LOCAL) "Email" else "Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        cursorColor = Color(0xFF8B5CF6),
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { authenticateWithCredentials() },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        cursorColor = Color(0xFF8B5CF6),
                    ),
                )

                authError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { authenticateWithCredentials() },
                    enabled = !isAuthenticating && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6),
                    ),
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isAuthenticating) "Signing in..." else "Sign In")
                }
            }

            3 -> {
                // Step 3: Done
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF22C55E),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Connected!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You can now discover and request media",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onSetupComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                        ),
                    ) {
                        Text("Start Discovering")
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSeerrSetupProgress(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("Connect", "Login", "Done").forEachIndexed { index, label ->
            val step = index + 1
            val isActive = step <= currentStep
            val isComplete = step < currentStep

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isActive) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White,
                        )
                    } else {
                        Text(
                            text = step.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MobileSeerrSetupStep(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF8B5CF6),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

