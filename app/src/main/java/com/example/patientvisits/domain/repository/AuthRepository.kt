package com.example.patientvisits.domain.repository

import kotlinx.coroutines.flow.Flow

sealed interface AuthResult {
    data object Success : AuthResult
    data object Rejected : AuthResult
    data object NetworkError : AuthResult
    data object ServerUnavailable : AuthResult
    data object Failure : AuthResult
}

interface AuthRepository {

    val isLoggedIn: Flow<Boolean>

    suspend fun login(email: String, password: String): AuthResult

    suspend fun signup(firstName: String, lastName: String, email: String, password: String): AuthResult

    suspend fun logout()
}
