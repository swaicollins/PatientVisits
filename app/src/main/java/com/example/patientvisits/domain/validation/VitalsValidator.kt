package com.example.patientvisits.domain.validation

import java.time.LocalDate

enum class VitalsField { VISIT_DATE, HEIGHT, WEIGHT }

data class VitalsForm(
    val visitDate: LocalDate? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null
)

object VitalsValidator {

    const val MIN_HEIGHT_CM = 30.0
    const val MAX_HEIGHT_CM = 300.0
    const val MIN_WEIGHT_KG = 1.0
    const val MAX_WEIGHT_KG = 700.0


    fun validate(
        form: VitalsForm,
        registrationDate: LocalDate,
        today: LocalDate,
        visitDateTaken: Boolean
    ): ValidationResult<VitalsField> {
        val errors = mutableMapOf<VitalsField, ValidationError>()

        val visit = form.visitDate
        if (visit == null) {
            errors[VitalsField.VISIT_DATE] = ValidationError.REQUIRED
        } else if (visit.isAfter(today)) {
            errors[VitalsField.VISIT_DATE] = ValidationError.DATE_IN_FUTURE
        } else if (visit.isBefore(registrationDate)) {
            errors[VitalsField.VISIT_DATE] = ValidationError.VISIT_BEFORE_REGISTRATION
        } else if (visitDateTaken) {
            errors[VitalsField.VISIT_DATE] = ValidationError.DUPLICATE_VISIT_DATE
        }

        checkRange(form.heightCm, MIN_HEIGHT_CM, MAX_HEIGHT_CM)?.let { errors[VitalsField.HEIGHT] = it }
        checkRange(form.weightKg, MIN_WEIGHT_KG, MAX_WEIGHT_KG)?.let { errors[VitalsField.WEIGHT] = it }

        return ValidationResult(errors)
    }

    private fun checkRange(value: Double?, min: Double, max: Double): ValidationError? = when {
        value == null -> ValidationError.REQUIRED
        !value.isFinite() || value < min || value > max -> ValidationError.OUT_OF_RANGE
        else -> null
    }
}
