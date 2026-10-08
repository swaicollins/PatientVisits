package com.example.patientvisits.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    private val validSignup = SignupForm(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com",
        password = "secret1",
        confirmPassword = "secret1"
    )

    @Test
    fun completeLogin_isValid() {
        assertTrue(AuthValidator.validateLogin(LoginForm("jane@example.com", "x")).isValid)
    }

    @Test
    fun emptyLogin_flagsBothFieldsAsRequired() {
        val result = AuthValidator.validateLogin(LoginForm())

        assertEquals(ValidationError.REQUIRED, result.errors[LoginField.EMAIL])
        assertEquals(ValidationError.REQUIRED, result.errors[LoginField.PASSWORD])
    }

    @Test
    fun malformedEmails_areRejected() {
        listOf("jane", "jane@", "@example.com", "jane@example", "ja ne@example.com").forEach {
            val result = AuthValidator.validateLogin(LoginForm(it, "x"))
            assertEquals(it, ValidationError.INVALID_EMAIL, result.errors[LoginField.EMAIL])
        }
    }

    @Test
    fun emailWithSurroundingSpaces_isAccepted() {
        assertTrue(AuthValidator.isValidEmail("  jane@example.com "))
    }

    @Test
    fun completeSignup_isValid() {
        assertTrue(AuthValidator.validateSignup(validSignup).isValid)
    }

    @Test
    fun emptySignup_flagsEveryField() {
        val result = AuthValidator.validateSignup(SignupForm())

        assertEquals(SignupField.entries.toSet(), result.errors.keys)
        assertTrue(result.errors.values.all { it == ValidationError.REQUIRED })
    }

    @Test
    fun passwordLengthBoundary() {
        val short = validSignup.copy(password = "12345", confirmPassword = "12345")
        val exact = validSignup.copy(password = "123456", confirmPassword = "123456")

        assertEquals(
            ValidationError.PASSWORD_TOO_SHORT,
            AuthValidator.validateSignup(short).errors[SignupField.PASSWORD]
        )
        assertTrue(AuthValidator.validateSignup(exact).isValid)
    }

    @Test
    fun mismatchedConfirmation_isRejected() {
        val result = AuthValidator.validateSignup(validSignup.copy(confirmPassword = "different"))

        assertEquals(ValidationError.PASSWORD_MISMATCH, result.errors[SignupField.CONFIRM_PASSWORD])
        assertFalse(result.errors.containsKey(SignupField.PASSWORD))
    }

    @Test
    fun blankNames_areRejected() {
        val result = AuthValidator.validateSignup(validSignup.copy(firstName = "  ", lastName = ""))

        assertEquals(ValidationError.REQUIRED, result.errors[SignupField.FIRST_NAME])
        assertEquals(ValidationError.REQUIRED, result.errors[SignupField.LAST_NAME])
    }
}
