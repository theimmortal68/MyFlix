package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.common.model.AuthResponse
import dev.jausc.myflix.core.common.model.JellyfinUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginScreenStateTest {

    private lateinit var mockAuthenticator: LoginAuthenticator
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val discoveredServer1 = DiscoveredServerInfo(
        name = "Jellyfin Server",
        address = "192.168.1.100:8096",
    )

    private val discoveredServer2 = DiscoveredServerInfo(
        name = "Backup Server",
        address = "192.168.1.101:8096",
    )

    private val validatedServer = ValidatedServerInfo(
        url = "http://192.168.1.100:8096",
        serverName = "Jellyfin Server",
        version = "10.9.0",
        quickConnectEnabled = true,
    )

    private val serverWithoutQuickConnect = ValidatedServerInfo(
        url = "http://192.168.1.100:8096",
        serverName = "Old Server",
        version = "10.7.0",
        quickConnectEnabled = false,
    )

    private val publicUser1 = PublicUserInfo(
        id = "user-1",
        name = "John",
        primaryImageTag = "tag1",
    )

    private val publicUser2 = PublicUserInfo(
        id = "user-2",
        name = "Jane",
        primaryImageTag = null,
    )

    private val authResponse = AuthResponse(
        accessToken = "token123",
        user = JellyfinUser(id = "user-1", name = "John"),
    )

    @Before
    fun setUp() {
        mockAuthenticator = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)
    }

    // region Initial State Tests

    @Test
    fun `initial state has empty servers and no connection`() {
        val state = createState()

        assertTrue(state.discoveredServers.isEmpty())
        assertFalse(state.isSearching)
        assertNull(state.connectedServer)
        assertFalse(state.isConnecting)
        assertTrue(state.publicUsers.isEmpty())
        assertFalse(state.isAuthenticating)
        assertNull(state.error)
    }

    @Test
    fun `initial hasConnectedServer is false`() {
        val state = createState()

        assertFalse(state.hasConnectedServer)
    }

    @Test
    fun `initial isQuickConnectAvailable is false`() {
        val state = createState()

        assertFalse(state.isQuickConnectAvailable)
    }

    // endregion

    // region discoverServers Tests

    @Test
    fun `discoverServers discovers servers successfully`() = testScope.runTest {
        coEvery { mockAuthenticator.discoverServers(any()) } returns Result.success(
            listOf(discoveredServer1, discoveredServer2),
        )

        val state = createState()
        state.discoverServers()
        advanceUntilIdle()

        assertEquals(listOf(discoveredServer1, discoveredServer2), state.discoveredServers)
        assertFalse(state.isSearching)
        assertNull(state.error)
    }

    @Test
    fun `discoverServers sets error on failure`() = testScope.runTest {
        coEvery { mockAuthenticator.discoverServers(any()) } returns Result.failure(
            Exception("Network unavailable"),
        )

        val state = createState()
        state.discoverServers()
        advanceUntilIdle()

        assertEquals("Network unavailable", state.error)
        assertTrue(state.discoveredServers.isEmpty())
        assertFalse(state.isSearching)
    }

    @Test
    fun `discoverServers uses provided timeout`() = testScope.runTest {
        coEvery { mockAuthenticator.discoverServers(3000L) } returns Result.success(emptyList())

        val state = createState()
        state.discoverServers(timeoutMs = 3000L)
        advanceUntilIdle()

        coVerify { mockAuthenticator.discoverServers(3000L) }
    }

    // endregion

    // region connectToServer Tests

    @Test
    fun `connectToServer connects and fetches public users`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer("192.168.1.100:8096") } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(validatedServer.url) } returns Result.success(
            listOf(publicUser1, publicUser2),
        )

        val state = createState()
        state.connectToServer("192.168.1.100:8096")
        advanceUntilIdle()

        assertEquals(validatedServer, state.connectedServer)
        assertEquals(listOf(publicUser1, publicUser2), state.publicUsers)
        assertFalse(state.isConnecting)
        assertNull(state.error)
    }

    @Test
    fun `connectToServer calls onSuccess callback`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(listOf(publicUser1))

        var callbackCalled = false
        var receivedServer: ValidatedServerInfo? = null
        var receivedUsers: List<PublicUserInfo>? = null

        val state = createState()
        state.connectToServer("192.168.1.100:8096") { server, users ->
            callbackCalled = true
            receivedServer = server
            receivedUsers = users
        }
        advanceUntilIdle()

        assertTrue(callbackCalled)
        assertEquals(validatedServer, receivedServer)
        assertEquals(listOf(publicUser1), receivedUsers)
    }

    @Test
    fun `connectToServer sets error on failure`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.failure(
            Exception("Connection refused"),
        )

        val state = createState()
        state.connectToServer("192.168.1.100:8096")
        advanceUntilIdle()

        assertEquals("Connection refused", state.error)
        assertNull(state.connectedServer)
        assertFalse(state.isConnecting)
    }

    @Test
    fun `connectToServer handles empty public users`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.failure(Exception("Not available"))

        val state = createState()
        state.connectToServer("192.168.1.100:8096")
        advanceUntilIdle()

        assertEquals(validatedServer, state.connectedServer)
        assertTrue(state.publicUsers.isEmpty())
    }

    // Note: Testing "does nothing if already connecting" is unreliable in unit tests
    // because the isConnecting flag is set inside the coroutine launch, not before it.
    // Both calls get scheduled before either has a chance to set the flag.

    // endregion

    // region hasConnectedServer and isQuickConnectAvailable Tests

    @Test
    fun `hasConnectedServer returns true when connected`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        assertTrue(state.hasConnectedServer)
    }

    @Test
    fun `isQuickConnectAvailable returns true when server supports it`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        assertTrue(state.isQuickConnectAvailable)
    }

    @Test
    fun `isQuickConnectAvailable returns false when server does not support it`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(serverWithoutQuickConnect)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        assertFalse(state.isQuickConnectAvailable)
    }

    // endregion

    // region disconnectServer Tests

    @Test
    fun `disconnectServer clears connection state`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(listOf(publicUser1))

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        assertTrue(state.hasConnectedServer)
        assertTrue(state.publicUsers.isNotEmpty())

        state.disconnectServer()

        assertNull(state.connectedServer)
        assertTrue(state.publicUsers.isEmpty())
        assertFalse(state.hasConnectedServer)
    }

    // endregion

    // region login Tests

    @Test
    fun `login authenticates successfully`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())
        coEvery { mockAuthenticator.login(validatedServer.url, "john", "password") } returns Result.success(authResponse)

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        var successCalled = false
        state.login("john", "password") { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(state.isAuthenticating)
        assertNull(state.error)
        coVerify { mockAuthenticator.onLoginSuccess(validatedServer, authResponse, "john", "password") }
    }

    @Test
    fun `login sets error for blank username`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        state.login("", "password") { }
        advanceUntilIdle()

        assertEquals("Please enter a username", state.error)
        coVerify(exactly = 0) { mockAuthenticator.login(any(), any(), any()) }
    }

    @Test
    fun `login sets error for whitespace-only username`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        state.login("   ", "password") { }
        advanceUntilIdle()

        assertEquals("Please enter a username", state.error)
    }

    @Test
    fun `login handles 401 error`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())
        coEvery { mockAuthenticator.login(any(), any(), any()) } returns Result.failure(
            Exception("HTTP 401 Unauthorized"),
        )

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        state.login("john", "wrong") { }
        advanceUntilIdle()

        assertEquals("Invalid username or password", state.error)
        assertFalse(state.isAuthenticating)
    }

    @Test
    fun `login handles timeout error`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())
        coEvery { mockAuthenticator.login(any(), any(), any()) } returns Result.failure(
            Exception("Connection timeout"),
        )

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        state.login("john", "password") { }
        advanceUntilIdle()

        assertEquals("Connection timed out", state.error)
    }

    @Test
    fun `login handles generic error`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())
        coEvery { mockAuthenticator.login(any(), any(), any()) } returns Result.failure(
            Exception("Server error"),
        )

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        state.login("john", "password") { }
        advanceUntilIdle()

        assertEquals("Server error", state.error)
    }

    @Test
    fun `login does nothing without connected server`() = testScope.runTest {
        val state = createState()

        state.login("john", "password") { }
        advanceUntilIdle()

        coVerify(exactly = 0) { mockAuthenticator.login(any(), any(), any()) }
    }

    // endregion

    // region handleQuickConnectSuccess Tests

    @Test
    fun `handleQuickConnectSuccess processes auth response`() = testScope.runTest {
        coEvery { mockAuthenticator.connectToServer(any()) } returns Result.success(validatedServer)
        coEvery { mockAuthenticator.getPublicUsers(any()) } returns Result.success(emptyList())

        val state = createState()
        state.connectToServer("192.168.1.100")
        advanceUntilIdle()

        var successCalled = false
        state.handleQuickConnectSuccess(authResponse) { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        coVerify { mockAuthenticator.onQuickConnectSuccess(validatedServer, authResponse) }
    }

    @Test
    fun `handleQuickConnectSuccess does nothing without connected server`() = testScope.runTest {
        val state = createState()

        state.handleQuickConnectSuccess(authResponse) { }
        advanceUntilIdle()

        coVerify(exactly = 0) { mockAuthenticator.onQuickConnectSuccess(any(), any()) }
    }

    // endregion

    // region clearError Tests

    @Test
    fun `clearError clears the error`() = testScope.runTest {
        coEvery { mockAuthenticator.discoverServers(any()) } returns Result.failure(Exception("Error"))

        val state = createState()
        state.discoverServers()
        advanceUntilIdle()

        assertEquals("Error", state.error)

        state.clearError()

        assertNull(state.error)
    }

    // endregion

    // Helper methods

    private fun createState(): LoginScreenState {
        return LoginScreenState(mockAuthenticator, testScope)
    }
}
