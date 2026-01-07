package dev.jausc.myflix.tv.ui.screens

import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import androidx.tv.material3.*
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.DiscoveredServer
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.QuickConnectFlowState
import dev.jausc.myflix.core.network.ValidatedServer
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Login flow matching official Jellyfin app:
 * 1. SERVER_DISCOVERY - Auto-discover or enter server address
 * 2. CONNECTING - Connecting to server...
 * 3. AUTH_CHOICE - Choose Quick Connect or Password login
 * 4. QUICK_CONNECT - Show code, wait for approval
 * 5. MANUAL_LOGIN - Username/password entry
 */
enum class LoginStep {
    SERVER_DISCOVERY,
    CONNECTING,
    AUTH_CHOICE,
    QUICK_CONNECT,
    MANUAL_LOGIN
}

@Composable
fun LoginScreen(
    appState: AppState,
    jellyfinClient: JellyfinClient,
    onLoginSuccess: () -> Unit
) {
    var currentStep by remember { mutableStateOf(LoginStep.SERVER_DISCOVERY) }
    var connectedServer by remember { mutableStateOf<ValidatedServer?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }
    var isSearching by remember { mutableStateOf(true) }
    
    // Use scope at this level so it survives screen transitions
    val scope = rememberCoroutineScope()
    
    // Auto-discover on launch
    LaunchedEffect(Unit) {
        isSearching = true
        discoveredServers = jellyfinClient.discoverServers(timeoutMs = 5000)
        isSearching = false
    }
    
    // Connection function that survives screen transitions
    fun connectToServer(address: String) {
        scope.launch {
            currentStep = LoginStep.CONNECTING
            connectionError = null
            
            jellyfinClient.connectToServer(address)
                .onSuccess { server ->
                    connectedServer = server
                    currentStep = LoginStep.AUTH_CHOICE
                }
                .onFailure { e ->
                    connectionError = e.message ?: "Connection failed"
                    currentStep = LoginStep.SERVER_DISCOVERY
                }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
        contentAlignment = Alignment.Center
    ) {
        when (currentStep) {
            LoginStep.SERVER_DISCOVERY -> {
                ServerDiscoveryScreen(
                    discoveredServers = discoveredServers,
                    isSearching = isSearching,
                    onRefresh = {
                        scope.launch {
                            isSearching = true
                            discoveredServers = jellyfinClient.discoverServers(timeoutMs = 5000)
                            isSearching = false
                        }
                    },
                    onConnect = { address -> connectToServer(address) }
                )
            }
            
            LoginStep.CONNECTING -> {
                ConnectingScreen()
            }
            
            LoginStep.AUTH_CHOICE -> {
                AuthChoiceScreen(
                    server = connectedServer!!,
                    onQuickConnect = {
                        currentStep = LoginStep.QUICK_CONNECT
                    },
                    onManualLogin = {
                        currentStep = LoginStep.MANUAL_LOGIN
                    },
                    onChangeServer = {
                        connectedServer = null
                        currentStep = LoginStep.SERVER_DISCOVERY
                    }
                )
            }
            
            LoginStep.QUICK_CONNECT -> {
                QuickConnectScreen(
                    server = connectedServer!!,
                    jellyfinClient = jellyfinClient,
                    appState = appState,
                    onLoginSuccess = onLoginSuccess,
                    onUsePassword = {
                        currentStep = LoginStep.MANUAL_LOGIN
                    },
                    onBack = {
                        currentStep = LoginStep.AUTH_CHOICE
                    }
                )
            }
            
            LoginStep.MANUAL_LOGIN -> {
                ManualLoginScreen(
                    server = connectedServer!!,
                    jellyfinClient = jellyfinClient,
                    appState = appState,
                    onLoginSuccess = onLoginSuccess,
                    onBack = {
                        currentStep = LoginStep.AUTH_CHOICE
                    }
                )
            }
        }
        
        // Show connection error as overlay
        connectionError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(TvColors.Surface, RoundedCornerShape(12.dp))
                        .padding(32.dp)
                ) {
                    Text(
                        text = "Connection Failed",
                        style = MaterialTheme.typography.titleLarge,
                        color = TvColors.Error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { connectionError = null }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerDiscoveryScreen(
    discoveredServers: List<DiscoveredServer>,
    isSearching: Boolean,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit
) {
    var serverAddress by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .width(500.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo/Title
        Text(
            text = "MyFlix",
            style = MaterialTheme.typography.displayMedium,
            color = TvColors.BluePrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Connect to Server",
            style = MaterialTheme.typography.headlineSmall,
            color = TvColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Server address input
        TvEditText(
            label = "Host",
            hint = "192.168.1.100",
            value = serverAddress,
            onValueChange = { serverAddress = it },
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        )
        
        // Connect button
        Button(
            onClick = { if (serverAddress.isNotBlank()) onConnect(serverAddress) },
            enabled = serverAddress.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.BluePrimary,
                disabledContainerColor = TvColors.Surface.copy(alpha = 0.5f)
            )
        ) {
            Text("Connect", fontSize = 16.sp)
        }
        
        // Choose server button (refresh discovery)
        Button(
            onClick = onRefresh,
            enabled = !isSearching,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.BluePrimary,
                focusedContainerColor = TvColors.BlueLight
            )
        ) {
            Text("Choose server", fontSize = 16.sp)
        }
        
        // Discovered servers
        if (isSearching) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvLoadingIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Searching for servers...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary
                )
            }
        } else if (discoveredServers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Found ${discoveredServers.size} server(s):",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary
            )
            
            discoveredServers.forEach { server ->
                Button(
                    onClick = { onConnect(server.address) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.Surface,
                        focusedContainerColor = TvColors.FocusedSurface
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(server.name, fontSize = 16.sp)
                        Text(
                            server.address,
                            fontSize = 12.sp,
                            color = TvColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectingScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TvLoadingIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        Text(
            text = "Connecting...",
            style = MaterialTheme.typography.titleLarge,
            color = TvColors.TextPrimary
        )
    }
}

@Composable
private fun AuthChoiceScreen(
    server: ValidatedServer,
    onQuickConnect: () -> Unit,
    onManualLogin: () -> Unit,
    onChangeServer: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(500.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo
        Text(
            text = "MyFlix",
            style = MaterialTheme.typography.displayMedium,
            color = TvColors.BluePrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please sign in",
            style = MaterialTheme.typography.headlineSmall,
            color = TvColors.TextPrimary
        )
        
        Text(
            text = server.serverInfo.serverName,
            style = MaterialTheme.typography.bodyLarge,
            color = TvColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sign In (manual) - Primary action
        Button(
            onClick = onManualLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.BluePrimary,
                focusedContainerColor = TvColors.BlueLight
            )
        ) {
            Text("Sign In", fontSize = 16.sp)
        }
        
        // Quick Connect - if available
        if (server.quickConnectEnabled) {
            Button(
                onClick = onQuickConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.Surface,
                    focusedContainerColor = TvColors.FocusedSurface
                )
            ) {
                Text("Use Quick Connect", fontSize = 16.sp)
            }
        }
        
        // Change Server
        Button(
            onClick = onChangeServer,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface
            )
        ) {
            Text("Change Server", fontSize = 16.sp)
        }
    }
}

@Composable
private fun QuickConnectScreen(
    server: ValidatedServer,
    jellyfinClient: JellyfinClient,
    appState: AppState,
    onLoginSuccess: () -> Unit,
    onUsePassword: () -> Unit,
    onBack: () -> Unit
) {
    var quickConnectState by remember { mutableStateOf<QuickConnectFlowState>(QuickConnectFlowState.Initializing) }
    var quickConnectJob by remember { mutableStateOf<Job?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(server.url) {
        quickConnectJob?.cancel()
        quickConnectJob = scope.launch {
            jellyfinClient.quickConnectFlow(server.url).collect { state ->
                quickConnectState = state
                
                if (state is QuickConnectFlowState.Authenticated) {
                    val response = state.authResponse
                    jellyfinClient.configure(
                        serverUrl = server.url,
                        accessToken = response.accessToken,
                        userId = response.user.id,
                        deviceId = jellyfinClient.deviceId
                    )
                    appState.login(server.url, response.accessToken, response.user.id)
                    onLoginSuccess()
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { quickConnectJob?.cancel() }
    }
    
    Column(
        modifier = Modifier
            .width(500.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quick Connect",
            style = MaterialTheme.typography.displaySmall,
            color = TvColors.BluePrimary
        )
        
        Text(
            text = server.serverInfo.serverName,
            style = MaterialTheme.typography.bodyLarge,
            color = TvColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (val state = quickConnectState) {
            is QuickConnectFlowState.Initializing -> {
                TvLoadingIndicator(modifier = Modifier.size(48.dp))
                Text("Initializing...", color = TvColors.TextSecondary)
            }
            
            is QuickConnectFlowState.NotAvailable -> {
                Text(
                    text = "Quick Connect is not available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.Error,
                    textAlign = TextAlign.Center
                )
            }
            
            is QuickConnectFlowState.WaitingForApproval -> {
                Text(
                    text = "Enter this code in Jellyfin:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextSecondary
                )
                
                Box(
                    modifier = Modifier
                        .background(TvColors.Surface, RoundedCornerShape(12.dp))
                        .border(2.dp, TvColors.BluePrimary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = state.code,
                        style = MaterialTheme.typography.displayLarge,
                        color = TvColors.BluePrimary,
                        letterSpacing = 8.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvLoadingIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Waiting for approval...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary
                    )
                }
            }
            
            is QuickConnectFlowState.Authenticating -> {
                TvLoadingIndicator(modifier = Modifier.size(48.dp))
                Text("Authenticating...", color = TvColors.TextSecondary)
            }
            
            is QuickConnectFlowState.Authenticated -> {
                TvLoadingIndicator(modifier = Modifier.size(48.dp))
                Text("Success!", color = TvColors.Success)
            }
            
            is QuickConnectFlowState.Error -> {
                Text(
                    text = state.message,
                    color = TvColors.Error,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                quickConnectJob?.cancel()
                onUsePassword()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface
            )
        ) {
            Text("Sign in with Password")
        }
        
        Button(
            onClick = {
                quickConnectJob?.cancel()
                onBack()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface
            )
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ManualLoginScreen(
    server: ValidatedServer,
    jellyfinClient: JellyfinClient,
    appState: AppState,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .width(500.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sign In",
            style = MaterialTheme.typography.displaySmall,
            color = TvColors.BluePrimary
        )
        
        Text(
            text = server.serverInfo.serverName,
            style = MaterialTheme.typography.bodyLarge,
            color = TvColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TvEditText(
            label = "User",
            hint = "",
            value = username,
            onValueChange = { 
                username = it
                errorMessage = null
            },
            inputType = EditorInfo.TYPE_CLASS_TEXT,
            enabled = !isLoading
        )
        
        TvEditText(
            label = "Password",
            hint = "",
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null
            },
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
            isPassword = true,
            enabled = !isLoading
        )
        
        errorMessage?.let { error ->
            Text(
                text = error,
                color = TvColors.Error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Sign In button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    
                    jellyfinClient.login(server.url, username, password)
                        .onSuccess { response ->
                            jellyfinClient.configure(
                                serverUrl = server.url,
                                accessToken = response.accessToken,
                                userId = response.user.id,
                                deviceId = jellyfinClient.deviceId
                            )
                            appState.login(server.url, response.accessToken, response.user.id)
                            onLoginSuccess()
                        }
                        .onFailure { e ->
                            errorMessage = when {
                                e.message?.contains("401") == true -> "Invalid username or password"
                                else -> e.message ?: "Login failed"
                            }
                        }
                    
                    isLoading = false
                }
            },
            enabled = !isLoading && username.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.BluePrimary,
                focusedContainerColor = TvColors.BlueLight
            )
        ) {
            if (isLoading) {
                TvLoadingIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign In", fontSize = 16.sp)
            }
        }
        
        // Use Quick Connect (if available)
        if (server.quickConnectEnabled) {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.Surface,
                    focusedContainerColor = TvColors.FocusedSurface
                )
            ) {
                Text("Use Quick Connect")
            }
        }
        
        // Back
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface
            )
        ) {
            Text("Back")
        }
    }
}

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
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        AndroidView(
            factory = { ctx ->
                createStyledEditText(ctx, hint, inputType, isPassword).apply {
                    setText(value)
                    doAfterTextChanged { text ->
                        onValueChange(text?.toString() ?: "")
                    }
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
                .height(56.dp)
        )
    }
}

private fun createStyledEditText(
    context: Context,
    hint: String,
    inputType: Int,
    isPassword: Boolean
): EditText {
    return EditText(context).apply {
        this.hint = hint
        this.inputType = inputType
        this.isSingleLine = true
        this.setTextColor(TvColors.TextPrimary.toArgb())
        this.setHintTextColor(TvColors.TextSecondary.toArgb())
        this.setBackgroundColor(TvColors.Surface.toArgb())
        this.setPadding(32, 24, 32, 24)
        this.textSize = 16f
        this.isFocusable = true
        this.isFocusableInTouchMode = true
        
        if (isPassword) {
            this.transformationMethod = PasswordTransformationMethod.getInstance()
        }
    }
}
