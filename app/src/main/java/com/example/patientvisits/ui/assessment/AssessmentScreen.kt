package com.example.patientvisits.ui.assessment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patientvisits.R
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.GeneralHealth
import com.example.patientvisits.domain.validation.AssessmentField
import com.example.patientvisits.ui.components.AppTextField
import com.example.patientvisits.ui.components.CollectEvents
import com.example.patientvisits.ui.components.DateField
import com.example.patientvisits.ui.components.ErrorBanner
import com.example.patientvisits.ui.components.FormScreen
import com.example.patientvisits.ui.components.RadioGroupField
import com.example.patientvisits.ui.components.ReadOnlyField
import com.example.patientvisits.ui.components.asMessage
import java.time.LocalDate

@Composable
fun AssessmentScreen(
    viewModel: AssessmentViewModel,
    type: AssessmentType,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            AssessmentViewModel.Event.AssessmentSaved -> onSaved()
        }
    }

    AssessmentContent(
        state = state,
        type = type,
        onVisitDateChange = viewModel::onVisitDateChange,
        onGeneralHealthChange = viewModel::onGeneralHealthChange,
        onYesNoChange = viewModel::onYesNoChange,
        onCommentsChange = viewModel::onCommentsChange,
        onClose = onClose,
        onSave = viewModel::onSave
    )
}

@Composable
fun AssessmentContent(
    state: AssessmentViewModel.UiState,
    type: AssessmentType,
    onVisitDateChange: (LocalDate) -> Unit,
    onGeneralHealthChange: (GeneralHealth) -> Unit,
    onYesNoChange: (Boolean) -> Unit,
    onCommentsChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    val patient = state.patient
    val form = state.form
    val errors = state.errors

    val title = when (type) {
        AssessmentType.GENERAL -> R.string.title_assessment_a
        AssessmentType.OVERWEIGHT -> R.string.title_assessment_b
    }
    val question = when (type) {
        AssessmentType.GENERAL -> R.string.question_diet
        AssessmentType.OVERWEIGHT -> R.string.question_drugs
    }

    FormScreen(
        title = stringResource(title),
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
            date = form.visitDate,
            onDateSelected = onVisitDateChange,
            error = errors[AssessmentField.VISIT_DATE].asMessage()
        )
        RadioGroupField(
            label = stringResource(R.string.label_general_health),
            options = GeneralHealth.entries,
            selected = form.generalHealth,
            optionLabel = {
                stringResource(if (it == GeneralHealth.GOOD) R.string.health_good else R.string.health_poor)
            },
            onSelected = onGeneralHealthChange,
            error = errors[AssessmentField.GENERAL_HEALTH].asMessage()
        )
        RadioGroupField(
            label = stringResource(question),
            options = listOf(true, false),
            selected = form.yesNoAnswer,
            optionLabel = { stringResource(if (it) R.string.answer_yes else R.string.answer_no) },
            onSelected = onYesNoChange,
            error = errors[AssessmentField.YES_NO_ANSWER].asMessage()
        )
        AppTextField(
            label = stringResource(R.string.label_comments),
            value = form.comments,
            onValueChange = onCommentsChange,
            imeAction = ImeAction.Default,
            singleLine = false,
            minLines = 3,
            error = errors[AssessmentField.COMMENTS].asMessage()
        )

        if (state.saveFailed) {
            ErrorBanner(stringResource(R.string.error_save_failed))
        }
    }
}
