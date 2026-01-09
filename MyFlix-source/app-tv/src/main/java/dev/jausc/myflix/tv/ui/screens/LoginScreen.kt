package dev.jausc.myflix.tv.ui.screens

import android.graphics.Bitmap
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.jausc.myflix.core.common.ui.MyFlixFonts
import dev.jausc.myflix.core.common.ui.MyFlixLogo
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.*
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Login flow:
 * 1. SERVER_SELECT - Auto-discover servers, manual entry option
 * 2. USER_SELECT - Show public users (if available) or skip to auth
 * 3. AUTH_METHOD - Quick Connect (with QR) or Password
 * 4. PASSWORD_ENTRY - Username/password form
 */
private enum class LoginStep {
    SERVER_SELECT,
    USER_SELECT,
    AUTH_METHOD,
    PASSWORD_ENTRY
}

@Composable
fun LoginScreen(
    appState: AppState,
    jellyfinClient: JellyfinClient,
    onLoginSuccess: () -> Unit
) {
    var currentStep by remember { mutableStateOf(LoginStep.SERVER_SELECT) }
    var connectedServer by remember { mutableStateOf<ValidatedServer?>(null) }
    var publicUsers by remember { mutableStateOf<List<PublicUser>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<PublicUser?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Auto-dismiss errors
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(5000)
            errorMessage = null
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        when (currentStep) {
            LoginStep.SERVER_SELECT -> ServerSelectScreen(
                jellyfinClient = jellyfinClient,
                onServerConnected = { server, users ->
                    connectedServer = server
                    publicUsers = users
                    currentStep = if (users.isNotEmpty()) LoginStep.USER_SELECT else LoginStep.AUTH_METHOD
                },
                onError = { errorMessage = it }
            )
            
            LoginStep.USER_SELECT -> UserSelectScreen(
                server = connectedServer!!,
                users = publicUsers,
                onUserSelected = { user ->
                    selectedUser = user
                    currentStep = LoginStep.AUTH_METHOD
                },
                onManualLogin = {
                    selectedUser = null
                    currentStep = LoginStep.AUTH_METHOD
                },
                onBack = {
                    connectedServer = null
                    currentStep = LoginStep.SERVER_SELECT
                }
            )
            
            LoginStep.AUTH_METHOD -> AuthMethodScreen(
                server = connectedServer!!,
                selectedUser = selectedUser,
                jellyfinClient = jellyfinClient,
                appState = appState,
                onLoginSuccess = onLoginSuccess,
                onPasswordLogin = { currentStep = LoginStep.PASSWORD_ENTRY },
                onBack = {
                    currentStep = if (publicUsers.isNotEmpty()) LoginStep.USER_SELECT else LoginStep.SERVER_SELECT
                    if (publicUsers.isEmpty()) connectedServer = null
                },
                onError = { errorMessage = it }
            )
            
            LoginStep.PASSWORD_ENTRY -> PasswordEntryScreen(
                server = connectedServer!!,
                prefilledUsername = selectedUser?.name,
                jellyfinClient = jellyfinClient,
                appState = appState,
                onLoginSuccess = onLoginSuccess,
                onBack = { currentStep = LoginStep.AUTH_METHOD },
                onError = { errorMessage = it }
            )
        }
        
        // Error snackbar
        errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .background(TvColors.Error.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(error, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ==================== Server Select ====================

@Composable
private fun ServerSelectScreen(
    jellyfinClient: JellyfinClient,
    onServerConnected: (ValidatedServer, List<PublicUser>) -> Unit,
    onError: (String) -> Unit
) {
    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }
    var isSearching by remember { mutableStateOf(true) }
    var isConnecting by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualAddress by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        isSearching = true
        discoveredServers = jellyfinClient.discoverServers(timeoutMs = 5000)
        isSearching = false
    }
    
    fun connectTo(address: String) {
        if (isConnecting) return
        scope.launch {
            isConnecting = true
            jellyfinClient.connectToServer(address)
                .onSuccess { server ->
                    val users = jellyfinClient.getPublicUsers(server.url).getOrDefault(emptyList())
                    onServerConnected(server, users)
                }
                .onFailure { onError(it.message ?: "Connection failed") }
            isConnecting = false
        }
    }
    
    Row(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Left - Branding
        Column(
            modifier = Modifier.weight(0.4f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            MyFlixLogo(
                modifier = Modifier.fillMaxWidth(),
                height = null  // Scale to fill width
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Connect to your Jellyfin server",
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.TextSecondary
            )
        }
        
        // Right - Server selection
        Column(
            modifier = Modifier.weight(0.6f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            if (showManualEntry) {
                Text("Enter Server Address", style = MaterialTheme.typography.titleLarge, color = TvColors.TextPrimary)
                Spacer(Modifier.height(24.dp))
                
                TvEditText(
                    label = "Server Address",
                    hint = "192.168.1.100 or jellyfin.example.com",
                    value = manualAddress,
                    onValueChange = { manualAddress = it },
                    inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI,
                    enabled = !isConnecting,
                    modifier = Modifier.width(400.dp)
                )
                Spacer(Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { connectTo(manualAddress) },
                        enabled = manualAddress.isNotBlank() && !isConnecting,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.BluePrimary,
                            focusedContainerColor = TvColors.BlueLight
                        )
                    ) {
                        if (isConnecting) {
                            TvLoadingIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Connect", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    
                    Button(
                        onClick = { showManualEntry = false },
                        enabled = !isConnecting,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.Surface,
                            focusedContainerColor = TvColors.FocusedSurface
                        )
                    ) {
                        Text("Back", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text("Select Server", style = MaterialTheme.typography.titleLarge, color = TvColors.TextPrimary)
                Spacer(Modifier.height(24.dp))
                
                if (isSearching) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvLoadingIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Searching for servers...", color = TvColors.TextSecondary)
                    }
                } else if (discoveredServers.isEmpty()) {
                    Text("No servers found on your network", color = TvColors.TextSecondary)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        discoveredServers.take(4).forEach { server ->
                            ServerCard(server, isConnecting) { connectTo(server.address) }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { showManualEntry = true },
                        enabled = !isConnecting,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.Surface,
                            focusedContainerColor = TvColors.FocusedSurface
                        )
                    ) {
                        Text("Enter Manually", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (!isSearching) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSearching = true
                                    discoveredServers = jellyfinClient.discoverServers(timeoutMs = 5000)
                                    isSearching = false
                                }
                            },
                            enabled = !isConnecting,
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.colors(
                                containerColor = TvColors.Surface,
                                focusedContainerColor = TvColors.FocusedSurface
                            )
                        ) {
                            Text("Refresh", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                if (isConnecting) {
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvLoadingIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Connecting...", color = TvColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: DiscoveredServer,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isConnecting,
        modifier = Modifier.width(140.dp).height(72.dp),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = server.address.removePrefix("http://").removePrefix("https://"),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== User Select ====================

@Composable
private fun UserSelectScreen(
    server: ValidatedServer,
    users: List<PublicUser>,
    onUserSelected: (PublicUser) -> Unit,
    onManualLogin: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Left - Server info
        Column(
            modifier = Modifier.weight(0.35f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            MyFlixLogo(modifier = Modifier.fillMaxWidth(), height = null)
            Spacer(Modifier.height(16.dp))
            Text(server.serverInfo.serverName, style = MaterialTheme.typography.headlineSmall, color = TvColors.TextPrimary)
            Text("v${server.serverInfo.version}", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextSecondary)
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.colors(containerColor = TvColors.Surface, focusedContainerColor = TvColors.FocusedSurface)
            ) {
                Text("Change Server", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        // Right - Users
        Column(
            modifier = Modifier.weight(0.65f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Who's watching?", style = MaterialTheme.typography.titleLarge, color = TvColors.TextPrimary)
            Spacer(Modifier.height(32.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                users.take(5).forEach { user ->
                    UserCard(user, server.url) { onUserSelected(user) }
                }
                OtherUserCard(onManualLogin)
            }
        }
    }
}

@Composable
private fun UserCard(user: PublicUser, serverUrl: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.colors(containerColor = TvColors.Surface, focusedContainerColor = TvColors.FocusedSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user.primaryImageTag != null) {
                AsyncImage(
                    model = "$serverUrl/Users/${user.id}/Images/Primary?quality=90",
                    contentDescription = user.name,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).background(TvColors.BluePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(user.name, style = MaterialTheme.typography.bodySmall, color = TvColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OtherUserCard(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.colors(containerColor = TvColors.Surface, focusedContainerColor = TvColors.FocusedSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(TvColors.SurfaceLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = TvColors.TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Text("Other", style = MaterialTheme.typography.bodySmall, color = TvColors.TextSecondary)
        }
    }
}

// ==================== Auth Method ====================

@Composable
private fun AuthMethodScreen(
    server: ValidatedServer,
    selectedUser: PublicUser?,
    jellyfinClient: JellyfinClient,
    appState: AppState,
    onLoginSuccess: () -> Unit,
    onPasswordLogin: () -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit
) {
    var quickConnectState by remember { mutableStateOf<QuickConnectFlowState?>(null) }
    var quickConnectJob by remember { mutableStateOf<Job?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(server) {
        if (server.quickConnectEnabled) {
            quickConnectJob = scope.launch {
                jellyfinClient.quickConnectFlow(server.url).collect { state ->
                    quickConnectState = state
                    when (state) {
                        is QuickConnectFlowState.WaitingForApproval -> {
                            qrBitmap = generateQrCode("${server.url}/web/#/quickconnect", 120)
                        }
                        is QuickConnectFlowState.Authenticated -> {
                            val response = state.authResponse
                            jellyfinClient.configure(server.url, response.accessToken, response.user.id, jellyfinClient.deviceId)
                            appState.login(server.url, response.accessToken, response.user.id)
                            onLoginSuccess()
                        }
                        is QuickConnectFlowState.Error -> onError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }
    
    DisposableEffect(Unit) { onDispose { quickConnectJob?.cancel() } }
    
    Row(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(64.dp)
    ) {
        // Left - Quick Connect
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (server.quickConnectEnabled) {
                Text("Quick Connect", style = MaterialTheme.typography.titleLarge, color = TvColors.TextPrimary)
                Spacer(Modifier.height(24.dp))
                
                when (val state = quickConnectState) {
                    is QuickConnectFlowState.WaitingForApproval -> {
                        // Code display
                        Box(
                            modifier = Modifier
                                .background(TvColors.Surface, RoundedCornerShape(12.dp))
                                .padding(horizontal = 48.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = state.code,
                                style = MaterialTheme.typography.displayMedium,
                                color = TvColors.BluePrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 12.sp
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "1. Scan the QR code below\n2. Log in if prompted\n3. Enter the code above",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        qrBitmap?.let { bitmap ->
                            Text("Scan to open Quick Connect:", style = MaterialTheme.typography.bodySmall, color = TvColors.TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.size(100.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp)
                            ) {
                                Image(bitmap.asImageBitmap(), "Server QR Code", Modifier.fillMaxSize())
                            }
                        }
                    }
                    is QuickConnectFlowState.Authenticating -> {
                        TvLoadingIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Authenticating...", color = TvColors.TextSecondary)
                    }
                    is QuickConnectFlowState.Initializing -> {
                        TvLoadingIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Initializing Quick Connect...", color = TvColors.TextSecondary)
                    }
                    else -> Text("Quick Connect unavailable", color = TvColors.TextSecondary)
                }
            } else {
                Text("Quick Connect", style = MaterialTheme.typography.titleLarge, color = TvColors.TextSecondary)
                Spacer(Modifier.height(24.dp))
                Text("Not enabled on this server", style = MaterialTheme.typography.bodyMedium, color = TvColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text("Enable in Dashboard → General", style = MaterialTheme.typography.bodySmall, color = TvColors.TextSecondary)
            }
        }
        
        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(0.6f)
                .align(Alignment.CenterVertically)
                .background(TvColors.SurfaceLight)
        )
        
        // Right - Password option
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign In", style = MaterialTheme.typography.titleLarge, color = TvColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            
            selectedUser?.let {
                Text("as ${it.name}", style = MaterialTheme.typography.bodyLarge, color = TvColors.TextSecondary)
                Spacer(Modifier.height(32.dp))
            }
            
            Button(
                onClick = onPasswordLogin,
                modifier = Modifier.width(200.dp).height(40.dp),
                colors = ButtonDefaults.colors(containerColor = TvColors.BluePrimary, focusedContainerColor = TvColors.BlueLight)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sign in with Password", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = { quickConnectJob?.cancel(); onBack() },
                modifier = Modifier.width(200.dp).height(40.dp),
                colors = ButtonDefaults.colors(containerColor = TvColors.Surface, focusedContainerColor = TvColors.FocusedSurface)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Back", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ==================== Password Entry ====================

@Composable
private fun PasswordEntryScreen(
    server: ValidatedServer,
    prefilledUsername: String?,
    jellyfinClient: JellyfinClient,
    appState: AppState,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit
) {
    var username by remember { mutableStateOf(prefilledUsername ?: "") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    fun doLogin() {
        if (username.isBlank()) { onError("Please enter a username"); return }
        scope.launch {
            isLoading = true
            jellyfinClient.login(server.url, username, password)
                .onSuccess { response ->
                    jellyfinClient.configure(server.url, response.accessToken, response.user.id, jellyfinClient.deviceId)
                    appState.login(server.url, response.accessToken, response.user.id)
                    onLoginSuccess()
                }
                .onFailure { e ->
                    onError(when {
                        e.message?.contains("401") == true -> "Invalid username or password"
                        e.message?.contains("timeout", true) == true -> "Connection timed out"
                        else -> e.message ?: "Login failed"
                    })
                }
            isLoading = false
        }
    }
    
    Row(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Left - Branding
        Column(
            modifier = Modifier.weight(0.4f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            MyFlixLogo(modifier = Modifier.fillMaxWidth(), height = null)
            Spacer(Modifier.height(16.dp))
            Text(server.serverInfo.serverName, style = MaterialTheme.typography.headlineSmall, color = TvColors.TextPrimary)
        }
        
        // Right - Form
        Column(
            modifier = Modifier.weight(0.6f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Sign In", style = MaterialTheme.typography.titleLarge, color = TvColors.TextPrimary)
            Spacer(Modifier.height(32.dp))
            
            TvEditText(
                label = "Username",
                hint = "",
                value = username,
                onValueChange = { username = it },
                inputType = EditorInfo.TYPE_CLASS_TEXT,
                enabled = !isLoading && prefilledUsername == null,
                modifier = Modifier.width(400.dp)
            )
            Spacer(Modifier.height(16.dp))
            
            TvEditText(
                label = "Password",
                hint = "",
                value = password,
                onValueChange = { password = it },
                inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
                isPassword = true,
                enabled = !isLoading,
                modifier = Modifier.width(400.dp)
            )
            Spacer(Modifier.height(32.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { doLogin() },
                    enabled = !isLoading && username.isNotBlank(),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.colors(containerColor = TvColors.BluePrimary, focusedContainerColor = TvColors.BlueLight)
                ) {
                    if (isLoading) {
                        TvLoadingIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Button(
                    onClick = onBack,
                    enabled = !isLoading,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.colors(containerColor = TvColors.Surface, focusedContainerColor = TvColors.FocusedSurface)
                ) {
                    Text("Back", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ==================== Utilities ====================

@Composable
private fun TvEditText(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    inputType: Int,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TvColors.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        
        AndroidView(
            factory = { ctx ->
                EditText(ctx).apply {
                    this.hint = hint
                    this.inputType = inputType
                    isSingleLine = true
                    setTextColor(TvColors.TextPrimary.toArgb())
                    setHintTextColor(TvColors.TextSecondary.toArgb())
                    setBackgroundColor(TvColors.Surface.toArgb())
                    setPadding(32, 16, 32, 16)
                    textSize = 18f
                    isFocusable = true
                    isFocusableInTouchMode = true
                    if (isPassword) transformationMethod = PasswordTransformationMethod.getInstance()
                    setText(value)
                    doAfterTextChanged { onValueChange(it?.toString() ?: "") }
                }
            },
            update = { editText ->
                editText.isEnabled = enabled
                editText.alpha = if (enabled) 1f else 0.5f
                if (editText.text.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

private fun generateQrCode(content: String, size: Int): Bitmap? = try {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
} catch (e: Exception) {
    android.util.Log.e("LoginScreen", "QR code generation failed", e)
    null
}
