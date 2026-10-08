package com.example.patientvisits.ui.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patientvisits.domain.BmiCalculator
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.BmiStatus
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.model.Vitals
import com.example.patientvisits.domain.repository.DuplicateVisitException
import com.example.patientvisits.domain.repository.PatientRepository
import com.example.patientvisits.domain.validation.ValidationError
import com.example.patientvisits.domain.validation.VitalsField
import com.example.patientvisits.domain.validation.VitalsForm
import com.example.patientvisits.domain.validation.VitalsValidator
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

class VitalsViewModel(
    private val repository: PatientRepository,
    private val patientId: String,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val patient: Patient? = null,
        val visitDate: LocalDate? = null,
        val heightText: String = "",
        val weightText: String = "",
        val errors: Map<VitalsField, ValidationError> = emptyMap(),
        val saving: Boolean = false,
        val saveFailed: Boolean = false
    ) {
        val bmi: Double?
            get() {
                val height = parseDecimal(heightText)
                val weight = parseDecimal(weightText)
                val plausible = height != null && weight != null &&
                    height in VitalsValidator.MIN_HEIGHT_CM..VitalsValidator.MAX_HEIGHT_CM &&
                    weight in VitalsValidator.MIN_WEIGHT_KG..VitalsValidator.MAX_WEIGHT_KG
                return if (plausible) BmiCalculator.calculate(height!!, weight!!) else null
            }

        val bmiStatus: BmiStatus? get() = bmi?.let(BmiCalculator::classify)
    }

    sealed interface Event {
        data class VitalsSaved(val assessmentType: AssessmentType, val visitDate: LocalDate) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val patient = repository.getPatient(patientId)
            _state.update { it.copy(loading = false, patient = patient, visitDate = today()) }
        }
    }

    fun onVisitDateChange(value: LocalDate) =
        _state.update { it.copy(visitDate = value, errors = it.errors - VitalsField.VISIT_DATE, saveFailed = false) }

    fun onHeightChange(value: String) =
        _state.update { it.copy(heightText = value, errors = it.errors - VitalsField.HEIGHT, saveFailed = false) }

    fun onWeightChange(value: String) =
        _state.update { it.copy(weightText = value, errors = it.errors - VitalsField.WEIGHT, saveFailed = false) }

    fun onSave() {
        val current = _state.value
        val patient = current.patient ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true, saveFailed = false) }

        viewModelScope.launch {
            try {
                val form = VitalsForm(
                    visitDate = current.visitDate,
                    heightCm = parseDecimal(current.heightText),
                    weightKg = parseDecimal(current.weightText)
                )
                val dateTaken = form.visitDate?.let { repository.hasVitalsOn(patientId, it) } ?: false

                val result = VitalsValidator.validate(form, patient.registrationDate, today(), dateTaken)
                if (!result.isValid) {
                    _state.update { it.copy(errors = result.errors) }
                    return@launch
                }

                val vitals = Vitals.create(patientId, form.visitDate!!, form.heightCm!!, form.weightKg!!)
                repository.saveVitals(vitals)
                _events.send(Event.VitalsSaved(AssessmentType.forBmi(vitals.bmi), vitals.visitDate))
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateVisitException) {
                _state.update { it.copy(errors = it.errors + (VitalsField.VISIT_DATE to ValidationError.DUPLICATE_VISIT_DATE)) }
            } catch (e: Exception) {
                _state.update { it.copy(saveFailed = true) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    companion object {
        fun factory(repository: PatientRepository, patientId: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { VitalsViewModel(repository, patientId) } }
    }
}

private fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
