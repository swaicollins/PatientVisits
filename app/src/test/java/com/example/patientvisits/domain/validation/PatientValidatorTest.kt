package com.example.patientvisits.domain.validation

import com.example.patientvisits.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PatientValidatorTest {

    private val today = LocalDate.of(2026, 10, 7)

    private val valid = PatientForm(
        patientId = "P-001",
        registrationDate = today,
        firstName = "Jane",
        lastName = "Doe",
        dateOfBirth = LocalDate.of(1990, 5, 20),
        gender = Gender.FEMALE
    )

    private fun validate(form: PatientForm, taken: Boolean = false) =
        PatientValidator.validate(form, today, patientIdTaken = taken)

    @Test
    fun completeForm_isValid() {
        assertTrue(validate(valid).isValid)
    }

    @Test
    fun emptyForm_flagsEveryMandatoryField() {
        val result = validate(PatientForm())
        assertEquals(PatientField.values().toSet(), result.errors.keys)
        assertTrue(result.errors.values.all { it == ValidationError.REQUIRED })
    }

    @Test
    fun blankPatientId_isRequired_evenWithOnlySpaces() {
        val result = validate(valid.copy(patientId = "   "))
        assertEquals(ValidationError.REQUIRED, result.errors[PatientField.PATIENT_ID])
    }

    @Test
    fun duplicatePatientId_isRejected() {
        val result = validate(valid, taken = true)
        assertEquals(ValidationError.DUPLICATE_PATIENT_ID, result.errors[PatientField.PATIENT_ID])
        assertEquals(1, result.errors.size)
    }

    @Test
    fun normalizeId_trimsWhitespace() {
        assertEquals("P-001", PatientValidator.normalizeId("  P-001 "))
    }

    @Test
    fun blankNames_areRequired() {
        val result = validate(valid.copy(firstName = " ", lastName = ""))
        assertEquals(ValidationError.REQUIRED, result.errors[PatientField.FIRST_NAME])
        assertEquals(ValidationError.REQUIRED, result.errors[PatientField.LAST_NAME])
    }

    @Test
    fun futureRegistrationDate_isRejected() {
        val result = validate(valid.copy(registrationDate = today.plusDays(1)))
        assertEquals(ValidationError.DATE_IN_FUTURE, result.errors[PatientField.REGISTRATION_DATE])
    }

    @Test
    fun registrationToday_isAllowed() {
        assertTrue(validate(valid.copy(registrationDate = today)).isValid)
    }

    @Test
    fun futureDateOfBirth_isRejected() {
        val result = validate(valid.copy(dateOfBirth = today.plusDays(1)))
        assertEquals(ValidationError.DATE_IN_FUTURE, result.errors[PatientField.DATE_OF_BIRTH])
    }

    @Test
    fun dobAfterRegistrationDate_isRejected() {
        val result = validate(
            valid.copy(registrationDate = LocalDate.of(2026, 1, 1), dateOfBirth = LocalDate.of(2026, 2, 1))
        )
        assertEquals(ValidationError.DOB_AFTER_REGISTRATION, result.errors[PatientField.DATE_OF_BIRTH])
    }

    @Test
    fun missingGender_isRequired() {
        val result = validate(valid.copy(gender = null))
        assertEquals(ValidationError.REQUIRED, result.errors[PatientField.GENDER])
    }
}
