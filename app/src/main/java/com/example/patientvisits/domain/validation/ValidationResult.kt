package com.example.patientvisits.domain.validation


enum class ValidationError {
    REQUIRED,
    DUPLICATE_PATIENT_ID,
    DATE_IN_FUTURE,
    DOB_AFTER_REGISTRATION,
    VISIT_BEFORE_REGISTRATION,
    DUPLICATE_VISIT_DATE,
    OUT_OF_RANGE,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH
}

data class ValidationResult<F>(val errors: Map<F, ValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()
}
