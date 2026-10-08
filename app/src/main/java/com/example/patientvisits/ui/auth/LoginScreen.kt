package com.example.patientvisits.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patientvisits.R
import com.example.patientvisits.domain.validation.LoginField
import com.example.patientvisits.ui.components.AppTextField
import com.example.patientvisits.ui.components.CollectEvents
import com.example.patientvisits.ui.components.ErrorBanner
import com.example.patientvisits.ui.components.asMessage

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            LoginViewModel.Event.LoggedIn -> onLoggedIn()
        }
    }

    AuthScreenLayout(
        title = stringResource(R.string.title_login),
        submitLabel = stringResource(if (state.loading) R.string.action_signing_in else R.string.action_login),
        switchLabel = stringResource(R.string.auth_go_to_signup),
        loading = state.loading,
        onSubmit = viewModel::onSubmit,
        onSwitch = onCreateAccount
    ) {
        AppTextField(
            label = stringResource(R.string.label_email),
            value = state.form.email,
            onValueChange = viewModel::onEmailChange,
            keyboardType = KeyboardType.Email,
            error = state.errors[LoginField.EMAIL].asMessage()
        )
        PasswordField(
            label = stringResource(R.string.label_password),
            value = state.form.password,
            onValueChange = viewModel::onPasswordChange,
            imeAction = ImeAction.Done,
            onDone = viewModel::onSubmit,
            error = state.errors[LoginField.PASSWORD].asMessage()
        )
        state.failure?.let { ErrorBanner(it.message(R.string.error_login_rejected)) }
    }
}
