package com.example.patientvisits.domain.validation

import com.example.patientvisits.domain.model.GeneralHealth
import java.time.LocalDate

enum class AssessmentField { VISIT_DATE, GENERAL_HEALTH, YES_NO_ANSWER, COMMENTS }

data class AssessmentForm(
    val visitDate: LocalDate? = null,
    val generalHealth: GeneralHealth? = null,
    val yesNoAnswer: Boolean? = null,
    val comments: String = ""
)

object AssessmentValidator {


    fun validate(
        form: AssessmentForm,
        registrationDate: LocalDate,
        today: LocalDate,
        alreadySubmittedForDate: Boolean
    ): ValidationResult<AssessmentField> {
        val errors = mutableMapOf<AssessmentField, ValidationError>()

        val visit = form.visitDate
        if (visit == null) {
            errors[AssessmentField.VISIT_DATE] = ValidationError.REQUIRED
        } else if (visit.isAfter(today)) {
            errors[AssessmentField.VISIT_DATE] = ValidationError.DATE_IN_FUTURE
        } else if (visit.isBefore(registrationDate)) {
            errors[AssessmentField.VISIT_DATE] = ValidationError.VISIT_BEFORE_REGISTRATION
        } else if (alreadySubmittedForDate) {
            errors[AssessmentField.VISIT_DATE] = ValidationError.DUPLICATE_VISIT_DATE
        }

        if (form.generalHealth == null) errors[AssessmentField.GENERAL_HEALTH] = ValidationError.REQUIRED
        if (form.yesNoAnswer == null) errors[AssessmentField.YES_NO_ANSWER] = ValidationError.REQUIRED
        if (form.comments.isBlank()) errors[AssessmentField.COMMENTS] = ValidationError.REQUIRED

        return ValidationResult(errors)
    }
}
