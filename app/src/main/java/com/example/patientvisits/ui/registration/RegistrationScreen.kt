package com.example.patientvisits.ui.registration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patientvisits.R
import com.example.patientvisits.domain.model.Gender
import com.example.patientvisits.domain.validation.PatientField
import com.example.patientvisits.ui.components.AppTextField
import com.example.patientvisits.ui.components.CollectEvents
import com.example.patientvisits.ui.components.DateField
import com.example.patientvisits.ui.components.DropdownField
import com.example.patientvisits.ui.components.ErrorBanner
import com.example.patientvisits.ui.components.FormScreen
import com.example.patientvisits.ui.components.asMessage

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel,
    onClose: () -> Unit,
    onRegistered: (patientId: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is RegistrationViewModel.Event.PatientRegistered -> onRegistered(event.patientId)
        }
    }

    RegistrationContent(
        state = state,
        onPatientIdChange = viewModel::onPatientIdChange,
        onRegistrationDateChange = viewModel::onRegistrationDateChange,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onGenderChange = viewModel::onGenderChange,
        onClose = onClose,
        onSave = viewModel::onSave
    )
}

@Composable
fun RegistrationContent(
    state: RegistrationViewModel.UiState,
    onPatientIdChange: (String) -> Unit,
    onRegistrationDateChange: (java.time.LocalDate) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDateOfBirthChange: (java.time.LocalDate) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    val form = state.form
    val errors = state.errors

    FormScreen(
        title = stringResource(R.string.title_registration),
        onClose = onClose,
        onSave = onSave,
        saving = state.saving
    ) {
        AppTextField(
            label = stringResource(R.string.label_patient_number),
            value = form.patientId,
            onValueChange = onPatientIdChange,
            error = errors[PatientField.PATIENT_ID].asMessage()
        )
        DateField(
            label = stringResource(R.string.label_registration_date),
            date = form.registrationDate,
            onDateSelected = onRegistrationDateChange,
            error = errors[PatientField.REGISTRATION_DATE].asMessage()
        )
        AppTextField(
            label = stringResource(R.string.label_first_name),
            value = form.firstName,
            onValueChange = onFirstNameChange,
            capitalization = KeyboardCapitalization.Words,
            error = errors[PatientField.FIRST_NAME].asMessage()
        )
        AppTextField(
            label = stringResource(R.string.label_last_name),
            value = form.lastName,
            onValueChange = onLastNameChange,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            error = errors[PatientField.LAST_NAME].asMessage()
        )
        DateField(
            label = stringResource(R.string.label_dob),
            date = form.dateOfBirth,
            onDateSelected = onDateOfBirthChange,
            error = errors[PatientField.DATE_OF_BIRTH].asMessage()
        )
        DropdownField(
            label = stringResource(R.string.label_gender),
            options = Gender.entries,
            selected = form.gender,
            optionLabel = { it.label },
            onSelected = onGenderChange,
            error = errors[PatientField.GENDER].asMessage()
        )
        if (state.saveFailed) {
            ErrorBanner(stringResource(R.string.error_save_failed))
        }
    }
}
