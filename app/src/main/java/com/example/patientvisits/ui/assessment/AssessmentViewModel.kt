package com.example.patientvisits.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patientvisits.domain.model.Assessment
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.GeneralAssessment
import com.example.patientvisits.domain.model.GeneralHealth
import com.example.patientvisits.domain.model.OverweightAssessment
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.repository.DuplicateVisitException
import com.example.patientvisits.domain.repository.PatientRepository
import com.example.patientvisits.domain.validation.AssessmentField
import com.example.patientvisits.domain.validation.AssessmentForm
import com.example.patientvisits.domain.validation.AssessmentValidator
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



class AssessmentViewModel(
    private val repository: PatientRepository,
    private val patientId: String,
    private val type: AssessmentType,
    initialVisitDate: LocalDate,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val patient: Patient? = null,
        val form: AssessmentForm = AssessmentForm(),
        val errors: Map<AssessmentField, ValidationError> = emptyMap(),
        val saving: Boolean = false,
        val saveFailed: Boolean = false
    )

    sealed interface Event {
        data object AssessmentSaved : Event
    }

    private val _state = MutableStateFlow(UiState(form = AssessmentForm(visitDate = initialVisitDate)))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val patient = repository.getPatient(patientId)
            _state.update { it.copy(loading = false, patient = patient) }
        }
    }

    private fun edit(field: AssessmentField, change: (AssessmentForm) -> AssessmentForm) {
        _state.update { it.copy(form = change(it.form), errors = it.errors - field, saveFailed = false) }
    }

    fun onVisitDateChange(value: LocalDate) = edit(AssessmentField.VISIT_DATE) { it.copy(visitDate = value) }
    fun onGeneralHealthChange(value: GeneralHealth) = edit(AssessmentField.GENERAL_HEALTH) { it.copy(generalHealth = value) }
    fun onYesNoChange(value: Boolean) = edit(AssessmentField.YES_NO_ANSWER) { it.copy(yesNoAnswer = value) }
    fun onCommentsChange(value: String) = edit(AssessmentField.COMMENTS) { it.copy(comments = value) }

    fun onSave() {
        val current = _state.value
        val patient = current.patient ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true, saveFailed = false) }

        viewModelScope.launch {
            try {
                val form = current.form
                val alreadySubmitted = form.visitDate
                    ?.let { repository.hasAssessmentOn(patientId, type, it) } ?: false

                val result = AssessmentValidator.validate(form, patient.registrationDate, today(), alreadySubmitted)
                if (!result.isValid) {
                    _state.update { it.copy(errors = result.errors) }
                    return@launch
                }

                repository.saveAssessment(buildAssessment(form))
                _events.send(Event.AssessmentSaved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateVisitException) {
                _state.update { it.copy(errors = it.errors + (AssessmentField.VISIT_DATE to ValidationError.DUPLICATE_VISIT_DATE)) }
            } catch (e: Exception) {
                _state.update { it.copy(saveFailed = true) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun buildAssessment(form: AssessmentForm): Assessment = when (type) {
        AssessmentType.GENERAL -> GeneralAssessment(
            patientId = patientId,
            visitDate = form.visitDate!!,
            generalHealth = form.generalHealth!!,
            everOnDiet = form.yesNoAnswer!!,
            comments = form.comments.trim()
        )
        AssessmentType.OVERWEIGHT -> OverweightAssessment(
            patientId = patientId,
            visitDate = form.visitDate!!,
            generalHealth = form.generalHealth!!,
            currentlyOnDrugs = form.yesNoAnswer!!,
            comments = form.comments.trim()
        )
    }

    companion object {
        fun factory(
            repository: PatientRepository,
            patientId: String,
            type: AssessmentType,
            initialVisitDate: LocalDate
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { AssessmentViewModel(repository, patientId, type, initialVisitDate) }
        }
    }
}
