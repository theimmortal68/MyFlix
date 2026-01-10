package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.DiscoveredServer
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.QuickConnectFlowState
import dev.jausc.myflix.core.network.ValidatedServer
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

    // Error dialog
    if (connectionError != null) {
        AlertDialog(
            onDismissRequest = { connectionError = null },
            title = { Text("Connection Failed") },
            text = { Text(connectionError!!) },
            confirmButton = {
                TextButton(onClick = { connectionError = null }) {
                    Text("OK")
                }
            }
        )
    }

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
            val server = connectedServer
            if (server == null) {
                // Guard against invalid state - return to discovery
                LaunchedEffect(Unit) { currentStep = LoginStep.SERVER_DISCOVERY }
            } else {
                AuthChoiceScreen(
                    server = server,
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
        }

        LoginStep.QUICK_CONNECT -> {
            val server = connectedServer
            if (server == null) {
                LaunchedEffect(Unit) { currentStep = LoginStep.SERVER_DISCOVERY }
            } else {
                QuickConnectScreen(
                    server = server,
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
        }

        LoginStep.MANUAL_LOGIN -> {
            val server = connectedServer
            if (server == null) {
                LaunchedEffect(Unit) { currentStep = LoginStep.SERVER_DISCOVERY }
            } else {
                ManualLoginScreen(
                    server = server,
                    jellyfinClient = jellyfinClient,
                    appState = appState,
                    onLoginSuccess = onLoginSuccess,
                    onBack = {
                        currentStep = LoginStep.AUTH_CHOICE
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerDiscoveryScreen(
    discoveredServers: List<DiscoveredServer>,
    isSearching: Boolean,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit
) {
    var serverAddress by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyFlix") },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect to Server",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Server address input
            OutlinedTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it },
                label = { Text("Host") },
                placeholder = { Text("192.168.1.100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { 
                        focusManager.clearFocus()
                        if (serverAddress.isNotBlank()) onConnect(serverAddress)
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Connect button
            Button(
                onClick = { if (serverAddress.isNotBlank()) onConnect(serverAddress) },
                enabled = serverAddress.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Choose server button
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isSearching,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose server")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Discovered servers
            if (isSearching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Searching for servers...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (discoveredServers.isNotEmpty()) {
                Text(
                    text = "Found ${discoveredServers.size} server(s):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                discoveredServers.forEach { server ->
                    Card(
                        onClick = { onConnect(server.address) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(server.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                server.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Connecting...")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthChoiceScreen(
    server: ValidatedServer,
    onQuickConnect: () -> Unit,
    onManualLogin: () -> Unit,
    onChangeServer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyFlix") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Please sign in",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = server.serverInfo.serverName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Sign In (manual) - Primary
            Button(
                onClick = onManualLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Connect - if available
            if (server.quickConnectEnabled) {
                OutlinedButton(
                    onClick = onQuickConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Quick Connect")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Change Server
            OutlinedButton(
                onClick = onChangeServer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Server")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Connect") },
                navigationIcon = {
                    IconButton(onClick = {
                        quickConnectJob?.cancel()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = server.serverInfo.serverName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = quickConnectState) {
                is QuickConnectFlowState.Initializing -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Initializing...")
                }

                is QuickConnectFlowState.NotAvailable -> {
                    Text(
                        text = "Quick Connect is not available on this server",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                is QuickConnectFlowState.WaitingForApproval -> {
                    Text(
                        text = "Enter this code in Jellyfin:",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Text(
                            text = state.code,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 8.sp,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Waiting for approval...")
                    }
                }

                is QuickConnectFlowState.Authenticating -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Authenticating...")
                }

                is QuickConnectFlowState.Authenticated -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Success!")
                }

                is QuickConnectFlowState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    quickConnectJob?.cancel()
                    onUsePassword()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Password")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = server.serverInfo.serverName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                label = { Text("User") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In")
                }
            }

            // Quick Connect option if available
            if (server.quickConnectEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Quick Connect")
                }
            }

            // Change Server
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Server")
            }
        }
    }
}
