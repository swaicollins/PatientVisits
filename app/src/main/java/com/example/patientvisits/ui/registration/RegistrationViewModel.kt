package com.example.patientvisits.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patientvisits.domain.model.Gender
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.repository.DuplicatePatientIdException
import com.example.patientvisits.domain.repository.PatientRepository
import com.example.patientvisits.domain.validation.PatientField
import com.example.patientvisits.domain.validation.PatientForm
import com.example.patientvisits.domain.validation.PatientValidator
import com.example.patientvisits.domain.validation.ValidationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class RegistrationViewModel(
    private val repository: PatientRepository,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    data class UiState(
        val form: PatientForm,
        val errors: Map<PatientField, ValidationError> = emptyMap(),
        val saving: Boolean = false,
        val saveFailed: Boolean = false
    )

    sealed interface Event {
        data class PatientRegistered(val patientId: String) : Event
    }

    private val _state = MutableStateFlow(UiState(form = PatientForm(registrationDate = today())))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private fun edit(field: PatientField, change: (PatientForm) -> PatientForm) {
        _state.update { it.copy(form = change(it.form), errors = it.errors - field, saveFailed = false) }
    }

    fun onPatientIdChange(value: String) = edit(PatientField.PATIENT_ID) { it.copy(patientId = value) }
    fun onRegistrationDateChange(value: LocalDate) = edit(PatientField.REGISTRATION_DATE) { it.copy(registrationDate = value) }
    fun onFirstNameChange(value: String) = edit(PatientField.FIRST_NAME) { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = edit(PatientField.LAST_NAME) { it.copy(lastName = value) }
    fun onDateOfBirthChange(value: LocalDate) = edit(PatientField.DATE_OF_BIRTH) { it.copy(dateOfBirth = value) }
    fun onGenderChange(value: Gender) = edit(PatientField.GENDER) { it.copy(gender = value) }

    fun onSave() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, saveFailed = false) }

        viewModelScope.launch {
            try {
                val form = _state.value.form
                val id = PatientValidator.normalizeId(form.patientId)
                val taken = id.isNotEmpty() && repository.isPatientIdTaken(id)

                val result = PatientValidator.validate(form, today(), patientIdTaken = taken)
                if (!result.isValid) {
                    _state.update { it.copy(errors = result.errors) }
                    return@launch
                }

                repository.registerPatient(
                    Patient(
                        patientId = id,
                        registrationDate = form.registrationDate!!,
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim(),
                        dateOfBirth = form.dateOfBirth!!,
                        gender = form.gender!!
                    )
                )
                _events.send(Event.PatientRegistered(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicatePatientIdException) {
                _state.update { it.copy(errors = it.errors + (PatientField.PATIENT_ID to ValidationError.DUPLICATE_PATIENT_ID)) }
            } catch (e: Exception) {
                _state.update { it.copy(saveFailed = true) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    companion object {
        fun factory(repository: PatientRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RegistrationViewModel(repository) }
        }
    }
}
