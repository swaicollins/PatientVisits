package com.example.patientvisits.data

import com.example.patientvisits.data.remote.LoginRequest
import com.example.patientvisits.data.remote.PatientApi
import com.example.patientvisits.data.remote.SignupRequest
import com.example.patientvisits.domain.repository.AuthRepository
import com.example.patientvisits.domain.repository.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SessionProvider {
    suspend fun ensureSession(): Boolean
    suspend fun invalidate()
}

class SessionManager(
    private val api: PatientApi,
    private val tokenStore: TokenStore
) : SessionProvider, AuthRepository {

    override val isLoggedIn: Flow<Boolean> =
        tokenStore.tokenFlow.map { !it.isNullOrBlank() }.distinctUntilChanged()

    override suspend fun ensureSession(): Boolean = !tokenStore.token().isNullOrBlank()

    override suspend fun invalidate() {
        tokenStore.clear()
    }

    override suspend fun logout() {
        tokenStore.clear()
    }

    override suspend fun login(email: String, password: String): AuthResult = guarded {
        val response = api.login(LoginRequest(email.trim(), password))
        val body = response.body()
        val token = body?.data?.accessToken
        when {
            response.isSuccessful && !token.isNullOrBlank() -> {
                tokenStore.save(token)
                AuthResult.Success
            }
            response.code() in REJECTED_CODES || (response.isSuccessful && body?.success == false) ->
                AuthResult.Rejected
            response.code() >= SERVER_ERROR_FROM -> AuthResult.ServerUnavailable
            else -> AuthResult.Failure
        }
    }

    override suspend fun signup(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthResult = guarded {
        val response = api.signup(
            SignupRequest(
                email = email.trim(),
                firstname = firstName.trim(),
                lastname = lastName.trim(),
                password = password
            )
        )
        when {
            response.isSuccessful && response.body()?.success != false -> login(email, password)
            response.code() in REJECTED_CODES || (response.isSuccessful && response.body()?.success == false) ->
                AuthResult.Rejected
            response.code() >= SERVER_ERROR_FROM -> AuthResult.ServerUnavailable
            else -> AuthResult.Failure
        }
    }

    private suspend fun guarded(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AuthResult.NetworkError
    } catch (e: Exception) {
        AuthResult.Failure
    }

    private companion object {
        val REJECTED_CODES = setOf(400, 401, 403, 409, 422)
        const val SERVER_ERROR_FROM = 500
    }
}
