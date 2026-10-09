package com.example.patientvisits.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patientvisits.domain.repository.AuthRepository
import com.example.patientvisits.domain.validation.AuthValidator
import com.example.patientvisits.domain.validation.SignupField
import com.example.patientvisits.domain.validation.SignupForm
import com.example.patientvisits.domain.validation.ValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignupViewModel(private val auth: AuthRepository) : ViewModel() {

    data class UiState(
        val form: SignupForm = SignupForm(),
        val errors: Map<SignupField, ValidationError> = emptyMap(),
        val loading: Boolean = false,
        val failure: AuthError? = null
    )

    sealed interface Event {
        data class SignedUp(val email: String) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private fun edit(field: SignupField, change: (SignupForm) -> SignupForm) {
        _state.update { it.copy(form = change(it.form), errors = it.errors - field, failure = null) }
    }

    fun onFirstNameChange(value: String) = edit(SignupField.FIRST_NAME) { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = edit(SignupField.LAST_NAME) { it.copy(lastName = value) }
    fun onEmailChange(value: String) = edit(SignupField.EMAIL) { it.copy(email = value) }
    fun onPasswordChange(value: String) = edit(SignupField.PASSWORD) { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) =
        edit(SignupField.CONFIRM_PASSWORD) { it.copy(confirmPassword = value) }

    fun onSubmit() {
        val current = _state.value
        if (current.loading) return

        val result = AuthValidator.validateSignup(current.form)
        if (!result.isValid) {
            _state.update { it.copy(errors = result.errors) }
            return
        }

        _state.update { it.copy(loading = true, failure = null) }
        viewModelScope.launch {
            val form = current.form
            val outcome = auth.signup(form.firstName, form.lastName, form.email, form.password)
            _state.update { it.copy(loading = false, failure = outcome.toError()) }
            if (outcome.toError() == null) _events.send(Event.SignedUp(form.email.trim()))
        }
    }

    companion object {
        fun factory(auth: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SignupViewModel(auth) }
        }
    }
}
