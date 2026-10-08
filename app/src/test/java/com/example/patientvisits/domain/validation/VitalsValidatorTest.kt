package com.example.patientvisits.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VitalsValidatorTest {

    private val today = LocalDate.of(2026, 10, 7)
    private val registered = LocalDate.of(2026, 10, 1)

    private val valid = VitalsForm(visitDate = today, heightCm = 170.0, weightKg = 70.0)

    private fun validate(form: VitalsForm, taken: Boolean = false) =
        VitalsValidator.validate(form, registered, today, visitDateTaken = taken)

    @Test
    fun completeForm_isValid() {
        assertTrue(validate(valid).isValid)
    }

    @Test
    fun emptyForm_flagsAllThreeFieldsAsRequired() {
        val result = validate(VitalsForm())
        assertEquals(VitalsField.values().toSet(), result.errors.keys)
        assertTrue(result.errors.values.all { it == ValidationError.REQUIRED })
    }

    @Test
    fun secondSubmissionOnTheSameDate_isRejected() {
        val result = validate(valid, taken = true)
        assertEquals(ValidationError.DUPLICATE_VISIT_DATE, result.errors[VitalsField.VISIT_DATE])
    }

    @Test
    fun submissionOnADifferentDate_isAllowed() {
        assertTrue(validate(valid.copy(visitDate = registered), taken = false).isValid)
    }

    @Test
    fun futureVisitDate_isRejected() {
        val result = validate(valid.copy(visitDate = today.plusDays(1)))
        assertEquals(ValidationError.DATE_IN_FUTURE, result.errors[VitalsField.VISIT_DATE])
    }

    @Test
    fun visitBeforeRegistration_isRejected() {
        val result = validate(valid.copy(visitDate = registered.minusDays(1)))
        assertEquals(ValidationError.VISIT_BEFORE_REGISTRATION, result.errors[VitalsField.VISIT_DATE])
    }

    @Test
    fun heightBoundaries() {
        assertTrue(validate(valid.copy(heightCm = VitalsValidator.MIN_HEIGHT_CM)).isValid)
        assertTrue(validate(valid.copy(heightCm = VitalsValidator.MAX_HEIGHT_CM)).isValid)
        assertEquals(
            ValidationError.OUT_OF_RANGE,
            validate(valid.copy(heightCm = VitalsValidator.MIN_HEIGHT_CM - 0.1)).errors[VitalsField.HEIGHT]
        )
        assertEquals(
            ValidationError.OUT_OF_RANGE,
            validate(valid.copy(heightCm = VitalsValidator.MAX_HEIGHT_CM + 0.1)).errors[VitalsField.HEIGHT]
        )
    }

    @Test
    fun heightTypedInMetres_isCaughtAsOutOfRange() {
        assertEquals(ValidationError.OUT_OF_RANGE, validate(valid.copy(heightCm = 1.7)).errors[VitalsField.HEIGHT])
    }

    @Test
    fun zeroAndNegativeWeight_areRejected() {
        assertEquals(ValidationError.OUT_OF_RANGE, validate(valid.copy(weightKg = 0.0)).errors[VitalsField.WEIGHT])
        assertEquals(ValidationError.OUT_OF_RANGE, validate(valid.copy(weightKg = -5.0)).errors[VitalsField.WEIGHT])
    }

    @Test
    fun nanAndInfinity_areRejected() {
        assertEquals(ValidationError.OUT_OF_RANGE, validate(valid.copy(heightCm = Double.NaN)).errors[VitalsField.HEIGHT])
        assertEquals(
            ValidationError.OUT_OF_RANGE,
            validate(valid.copy(weightKg = Double.POSITIVE_INFINITY)).errors[VitalsField.WEIGHT]
        )
    }
}
