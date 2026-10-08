package com.example.patientvisits.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.patientvisits.R
import com.example.patientvisits.domain.repository.AuthResult

enum class AuthError { REJECTED, NETWORK, SERVER, UNKNOWN }

fun AuthResult.toError(): AuthError? = when (this) {
    AuthResult.Success -> null
    AuthResult.Rejected -> AuthError.REJECTED
    AuthResult.NetworkError -> AuthError.NETWORK
    AuthResult.ServerUnavailable -> AuthError.SERVER
    AuthResult.Failure -> AuthError.UNKNOWN
}

@Composable
fun AuthError.message(rejectedMessage: Int): String = when (this) {
    AuthError.REJECTED -> stringResource(rejectedMessage)
    AuthError.NETWORK -> stringResource(R.string.error_network)
    AuthError.SERVER -> stringResource(R.string.error_server_unavailable)
    AuthError.UNKNOWN -> stringResource(R.string.error_auth_unknown)
}
