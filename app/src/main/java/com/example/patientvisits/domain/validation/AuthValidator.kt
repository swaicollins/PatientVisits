package com.example.patientvisits.domain.validation

enum class LoginField { EMAIL, PASSWORD }

enum class SignupField { FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD }

data class LoginForm(
    val email: String = "",
    val password: String = ""
)

data class SignupForm(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)

object AuthValidator {

    const val MIN_PASSWORD_LENGTH = 6

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(raw: String): Boolean = emailPattern.matches(raw.trim())

    fun validateLogin(form: LoginForm): ValidationResult<LoginField> {
        val errors = mutableMapOf<LoginField, ValidationError>()
        emailError(form.email)?.let { errors[LoginField.EMAIL] = it }
        if (form.password.isEmpty()) errors[LoginField.PASSWORD] = ValidationError.REQUIRED
        return ValidationResult(errors)
    }

    fun validateSignup(form: SignupForm): ValidationResult<SignupField> {
        val errors = mutableMapOf<SignupField, ValidationError>()
        if (form.firstName.isBlank()) errors[SignupField.FIRST_NAME] = ValidationError.REQUIRED
        if (form.lastName.isBlank()) errors[SignupField.LAST_NAME] = ValidationError.REQUIRED
        emailError(form.email)?.let { errors[SignupField.EMAIL] = it }

        if (form.password.isEmpty()) {
            errors[SignupField.PASSWORD] = ValidationError.REQUIRED
        } else if (form.password.length < MIN_PASSWORD_LENGTH) {
            errors[SignupField.PASSWORD] = ValidationError.PASSWORD_TOO_SHORT
        }

        if (form.confirmPassword.isEmpty()) {
            errors[SignupField.CONFIRM_PASSWORD] = ValidationError.REQUIRED
        } else if (form.confirmPassword != form.password) {
            errors[SignupField.CONFIRM_PASSWORD] = ValidationError.PASSWORD_MISMATCH
        }
        return ValidationResult(errors)
    }

    private fun emailError(raw: String): ValidationError? = when {
        raw.isBlank() -> ValidationError.REQUIRED
        !isValidEmail(raw) -> ValidationError.INVALID_EMAIL
        else -> null
    }
}
