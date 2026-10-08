package com.example.patientvisits.domain.validation

import com.example.patientvisits.domain.model.GeneralHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AssessmentValidatorTest {

    private val today = LocalDate.of(2026, 10, 7)
    private val registered = LocalDate.of(2026, 10, 1)

    private val valid = AssessmentForm(
        visitDate = today,
        generalHealth = GeneralHealth.GOOD,
        yesNoAnswer = false,
        comments = "No concerns"
    )

    private fun validate(form: AssessmentForm, alreadySubmitted: Boolean = false) =
        AssessmentValidator.validate(form, registered, today, alreadySubmittedForDate = alreadySubmitted)

    @Test
    fun completeForm_isValid() {
        assertTrue(validate(valid).isValid)
    }

    @Test
    fun emptyForm_flagsAllFieldsAsRequired() {
        val result = validate(AssessmentForm())
        assertEquals(AssessmentField.values().toSet(), result.errors.keys)
        assertTrue(result.errors.values.all { it == ValidationError.REQUIRED })
    }

    @Test
    fun answerNo_isNotTheSameAsUnanswered() {
        assertTrue(validate(valid.copy(yesNoAnswer = false)).isValid)
        assertEquals(
            ValidationError.REQUIRED,
            validate(valid.copy(yesNoAnswer = null)).errors[AssessmentField.YES_NO_ANSWER]
        )
    }

    @Test
    fun blankComments_areRejected() {
        assertEquals(
            ValidationError.REQUIRED,
            validate(valid.copy(comments = "   ")).errors[AssessmentField.COMMENTS]
        )
    }

    @Test
    fun secondSubmissionOnTheSameDate_isRejected() {
        val result = validate(valid, alreadySubmitted = true)
        assertEquals(ValidationError.DUPLICATE_VISIT_DATE, result.errors[AssessmentField.VISIT_DATE])
    }

    @Test
    fun futureVisitDate_isRejected() {
        val result = validate(valid.copy(visitDate = today.plusDays(1)))
        assertEquals(ValidationError.DATE_IN_FUTURE, result.errors[AssessmentField.VISIT_DATE])
    }

    @Test
    fun visitBeforeRegistration_isRejected() {
        val result = validate(valid.copy(visitDate = registered.minusDays(1)))
        assertEquals(ValidationError.VISIT_BEFORE_REGISTRATION, result.errors[AssessmentField.VISIT_DATE])
    }
}
