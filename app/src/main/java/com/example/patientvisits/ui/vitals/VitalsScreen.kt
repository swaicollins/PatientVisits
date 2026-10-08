package com.example.patientvisits.ui.vitals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patientvisits.R
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.validation.VitalsField
import com.example.patientvisits.domain.validation.VitalsValidator
import com.example.patientvisits.ui.components.AppTextField
import com.example.patientvisits.ui.components.CollectEvents
import com.example.patientvisits.ui.components.DateField
import com.example.patientvisits.ui.components.ErrorBanner
import com.example.patientvisits.ui.components.FormScreen
import com.example.patientvisits.ui.components.ReadOnlyField
import com.example.patientvisits.ui.components.asMessage
import java.time.LocalDate

@Composable
fun VitalsScreen(
    viewModel: VitalsViewModel,
    onClose: () -> Unit,
    onSaved: (AssessmentType, LocalDate) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is VitalsViewModel.Event.VitalsSaved -> onSaved(event.assessmentType, event.visitDate)
        }
    }

    VitalsContent(
        state = state,
        onVisitDateChange = viewModel::onVisitDateChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onClose = onClose,
        onSave = viewModel::onSave
    )
}

@Composable
fun VitalsContent(
    state: VitalsViewModel.UiState,
    onVisitDateChange: (LocalDate) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    val patient = state.patient
    val errors = state.errors
    val heightRange = VitalsValidator.MIN_HEIGHT_CM..VitalsValidator.MAX_HEIGHT_CM
    val weightRange = VitalsValidator.MIN_WEIGHT_KG..VitalsValidator.MAX_WEIGHT_KG

    FormScreen(
        title = stringResource(R.string.title_vitals),
        onClose = onClose,
        onSave = onSave,
        saving = state.saving,
        saveEnabled = patient != null
    ) {
        if (!state.loading && patient == null) {
            ErrorBanner(stringResource(R.string.error_patient_not_found))
            return@FormScreen
        }

        ReadOnlyField(
            label = stringResource(R.string.label_patient_name),
            value = patient?.fullName.orEmpty()
        )
        DateField(
            label = stringResource(R.string.label_visit_date),
            date = state.visitDate,
            onDateSelected = onVisitDateChange,
            error = errors[VitalsField.VISIT_DATE].asMessage()
        )
        AppTextField(
            label = stringResource(R.string.label_height),
            value = state.heightText,
            onValueChange = onHeightChange,
            keyboardType = KeyboardType.Decimal,
            suffix = stringResource(R.string.unit_cm),
            error = errors[VitalsField.HEIGHT].asMessage(heightRange)
        )
        AppTextField(
            label = stringResource(R.string.label_weight),
            value = state.weightText,
            onValueChange = onWeightChange,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            suffix = stringResource(R.string.unit_kg),
            error = errors[VitalsField.WEIGHT].asMessage(weightRange)
        )

        val bmi = state.bmi
        val status = state.bmiStatus
        ReadOnlyField(
            label = stringResource(R.string.label_bmi),
            value = if (bmi != null && status != null) "%.1f  •  %s".format(bmi, status.label) else "—"
        )

        if (state.saveFailed) {
            ErrorBanner(stringResource(R.string.error_save_failed))
        }
    }
}
