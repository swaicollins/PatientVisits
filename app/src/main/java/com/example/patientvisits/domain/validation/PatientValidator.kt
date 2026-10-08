package com.example.patientvisits.domain.validation

import com.example.patientvisits.domain.model.Gender
import java.time.LocalDate

enum class PatientField { PATIENT_ID, REGISTRATION_DATE, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, GENDER }

data class PatientForm(
    val patientId: String = "",
    val registrationDate: LocalDate? = null,
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null
)

object PatientValidator {

    fun normalizeId(raw: String): String = raw.trim()


    fun validate(
        form: PatientForm,
        today: LocalDate,
        patientIdTaken: Boolean
    ): ValidationResult<PatientField> {
        val errors = mutableMapOf<PatientField, ValidationError>()

        if (normalizeId(form.patientId).isEmpty()) {
            errors[PatientField.PATIENT_ID] = ValidationError.REQUIRED
        } else if (patientIdTaken) {
            errors[PatientField.PATIENT_ID] = ValidationError.DUPLICATE_PATIENT_ID
        }

        val registration = form.registrationDate
        if (registration == null) {
            errors[PatientField.REGISTRATION_DATE] = ValidationError.REQUIRED
        } else if (registration.isAfter(today)) {
            errors[PatientField.REGISTRATION_DATE] = ValidationError.DATE_IN_FUTURE
        }

        if (form.firstName.isBlank()) errors[PatientField.FIRST_NAME] = ValidationError.REQUIRED
        if (form.lastName.isBlank()) errors[PatientField.LAST_NAME] = ValidationError.REQUIRED

        val dob = form.dateOfBirth
        if (dob == null) {
            errors[PatientField.DATE_OF_BIRTH] = ValidationError.REQUIRED
        } else if (dob.isAfter(today)) {
            errors[PatientField.DATE_OF_BIRTH] = ValidationError.DATE_IN_FUTURE
        } else if (registration != null && dob.isAfter(registration)) {
            errors[PatientField.DATE_OF_BIRTH] = ValidationError.DOB_AFTER_REGISTRATION
        }

        if (form.gender == null) errors[PatientField.GENDER] = ValidationError.REQUIRED

        return ValidationResult(errors)
    }
}
