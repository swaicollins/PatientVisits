package com.example.patientvisits.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patientvisits.domain.repository.AuthRepository
import com.example.patientvisits.domain.validation.AuthValidator
import com.example.patientvisits.domain.validation.LoginField
import com.example.patientvisits.domain.validation.LoginForm
import com.example.patientvisits.domain.validation.ValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    data class UiState(
        val form: LoginForm = LoginForm(),
        val errors: Map<LoginField, ValidationError> = emptyMap(),
        val loading: Boolean = false,
        val failure: AuthError? = null
    )

    sealed interface Event {
        data object LoggedIn : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    fun onEmailChange(value: String) = _state.update {
        it.copy(form = it.form.copy(email = value), errors = it.errors - LoginField.EMAIL, failure = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(form = it.form.copy(password = value), errors = it.errors - LoginField.PASSWORD, failure = null)
    }

    fun onSubmit() {
        val current = _state.value
        if (current.loading) return

        val result = AuthValidator.validateLogin(current.form)
        if (!result.isValid) {
            _state.update { it.copy(errors = result.errors) }
            return
        }

        _state.update { it.copy(loading = true, failure = null) }
        viewModelScope.launch {
            val outcome = auth.login(current.form.email, current.form.password)
            _state.update { it.copy(loading = false, failure = outcome.toError()) }
            if (outcome.toError() == null) _events.send(Event.LoggedIn)
        }
    }

    companion object {
        fun factory(auth: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(auth) }
        }
    }
}
