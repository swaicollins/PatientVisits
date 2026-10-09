package com.example.patientvisits.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patientvisits.R
import com.example.patientvisits.domain.validation.SignupField
import com.example.patientvisits.ui.components.AppTextField
import com.example.patientvisits.ui.components.CollectEvents
import com.example.patientvisits.ui.components.ErrorBanner
import com.example.patientvisits.ui.components.asMessage

@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    onSignedUp: (email: String) -> Unit,
    onHaveAccount: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is SignupViewModel.Event.SignedUp -> onSignedUp(event.email)
        }
    }

    AuthScreenLayout(
        title = stringResource(R.string.title_signup),
        submitLabel = stringResource(if (state.loading) R.string.action_signing_up else R.string.action_signup),
        switchLabel = stringResource(R.string.auth_go_to_login),
        loading = state.loading,
        onSubmit = viewModel::onSubmit,
        onSwitch = onHaveAccount
    ) {
        AppTextField(
            label = stringResource(R.string.label_first_name),
            value = state.form.firstName,
            onValueChange = viewModel::onFirstNameChange,
            capitalization = KeyboardCapitalization.Words,
            error = state.errors[SignupField.FIRST_NAME].asMessage()
        )
        AppTextField(
            label = stringResource(R.string.label_last_name),
            value = state.form.lastName,
            onValueChange = viewModel::onLastNameChange,
            capitalization = KeyboardCapitalization.Words,
            error = state.errors[SignupField.LAST_NAME].asMessage()
        )
        AppTextField(
            label = stringResource(R.string.label_email),
            value = state.form.email,
            onValueChange = viewModel::onEmailChange,
            keyboardType = KeyboardType.Email,
            error = state.errors[SignupField.EMAIL].asMessage()
        )
        PasswordField(
            label = stringResource(R.string.label_password),
            value = state.form.password,
            onValueChange = viewModel::onPasswordChange,
            error = state.errors[SignupField.PASSWORD].asMessage()
        )
        PasswordField(
            label = stringResource(R.string.label_confirm_password),
            value = state.form.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            imeAction = ImeAction.Done,
            onDone = viewModel::onSubmit,
            error = state.errors[SignupField.CONFIRM_PASSWORD].asMessage()
        )
        state.failure?.let { ErrorBanner(it.message(R.string.error_signup_rejected)) }
    }
}
