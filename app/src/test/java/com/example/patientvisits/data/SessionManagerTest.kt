package com.example.patientvisits.data

import com.example.patientvisits.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private class MemoryTokenStore : TokenStore {
        private val current = MutableStateFlow<String?>(null)
        override val tokenFlow: Flow<String?> = current
        override suspend fun token(): String? = current.value
        override suspend fun save(token: String) {
            current.value = token
        }
        override suspend fun clear() {
            current.value = null
        }
    }

    private lateinit var api: FakePatientApi
    private lateinit var store: MemoryTokenStore
    private lateinit var manager: SessionManager

    @Before
    fun setUp() {
        api = FakePatientApi()
        store = MemoryTokenStore()
        manager = SessionManager(api, store)
    }

    @Test
    fun login_success_storesTokenAndReportsLoggedIn() = runTest {
        val result = manager.login("  user@example.com ", "secret1")

        assertEquals(AuthResult.Success, result)
        assertEquals("token-abc", store.token())
        assertEquals("user@example.com", api.loginRequests.single().email)
        assertTrue(manager.isLoggedIn.first())
        assertTrue(manager.ensureSession())
    }

    @Test
    fun login_withBadCredentials_isRejectedAndStoresNothing() = runTest {
        api.loginCode = 401

        assertEquals(AuthResult.Rejected, manager.login("user@example.com", "wrong"))
        assertNull(store.token())
        assertFalse(manager.isLoggedIn.first())
    }

    @Test
    fun login_whenOffline_reportsNetworkError() = runTest {
        api.networkDown = true

        assertEquals(AuthResult.NetworkError, manager.login("user@example.com", "secret1"))
        assertNull(store.token())
    }

    @Test
    fun login_onServerError_reportsServerUnavailableNotARejection() = runTest {
        api.loginCode = 503

        assertEquals(AuthResult.ServerUnavailable, manager.login("user@example.com", "secret1"))
        assertNull(store.token())
    }

    @Test
    fun login_onUnexpectedClientError_isAGenericFailure() = runTest {
        api.loginCode = 404

        assertEquals(AuthResult.Failure, manager.login("user@example.com", "secret1"))
    }

    @Test
    fun signup_whenServerIsDown_reportsServerUnavailableWithoutLoggingIn() = runTest {
        api.signupCode = 503

        assertEquals(AuthResult.ServerUnavailable, manager.signup("Jane", "Doe", "jane@example.com", "secret1"))
        assertEquals(listOf("signup"), api.calls)
    }

    @Test
    fun signup_success_logsTheNewUserInStraightAway() = runTest {
        val result = manager.signup(" Jane ", "Doe", "jane@example.com", "secret1")

        assertEquals(AuthResult.Success, result)
        assertEquals(listOf("signup", "login"), api.calls)
        val sent = api.signupRequests.single()
        assertEquals("Jane", sent.firstname)
        assertEquals("Doe", sent.lastname)
        assertEquals("token-abc", store.token())
    }

    @Test
    fun signup_whenEmailTaken_isRejectedWithoutLoggingIn() = runTest {
        api.signupCode = 422

        assertEquals(AuthResult.Rejected, manager.signup("Jane", "Doe", "jane@example.com", "secret1"))
        assertEquals(listOf("signup"), api.calls)
        assertNull(store.token())
    }

    @Test
    fun logout_clearsTheSession() = runTest {
        manager.login("user@example.com", "secret1")

        manager.logout()

        assertNull(store.token())
        assertFalse(manager.isLoggedIn.first())
        assertFalse(manager.ensureSession())
    }

    @Test
    fun invalidate_dropsTheSessionSoTheUserMustSignInAgain() = runTest {
        manager.login("user@example.com", "secret1")

        manager.invalidate()

        assertFalse(manager.isLoggedIn.first())
    }
}
