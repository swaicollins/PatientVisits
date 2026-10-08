package com.example.patientvisits.domain

import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.BmiStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BmiCalculatorTest {


    @Test
    fun calculate_usesCentimetresAndKilograms() {
        assertEquals(25.0, BmiCalculator.calculate(heightCm = 100.0, weightKg = 25.0), 0.0)
        assertEquals(25.0, BmiCalculator.calculate(heightCm = 200.0, weightKg = 100.0), 0.0)
    }

    @Test
    fun calculate_roundsToOneDecimalPlace() {
        assertEquals(24.2, BmiCalculator.calculate(170.0, 70.0), 0.0)
        assertEquals(22.9, BmiCalculator.calculate(175.0, 70.0), 0.0)
    }

    @Test
    fun calculate_roundsHalfUp_soDisplayedBmiMatchesClassification() {
        assertEquals(24.9, BmiCalculator.calculate(200.0, 99.7), 0.0)
        assertEquals(25.0, BmiCalculator.calculate(200.0, 99.8), 0.0)
    }

    @Test
    fun calculate_rejectsZeroHeight() {
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.calculate(0.0, 70.0) }
    }

    @Test
    fun calculate_rejectsNegativeWeight() {
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.calculate(170.0, -1.0) }
    }

    @Test
    fun calculate_rejectsNaNAndInfinity() {
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.calculate(Double.NaN, 70.0) }
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.calculate(170.0, Double.POSITIVE_INFINITY) }
    }


    @Test
    fun classify_justBelow18_5_isUnderweight() {
        assertEquals(BmiStatus.UNDERWEIGHT, BmiCalculator.classify(18.49))
    }

    @Test
    fun classify_exactly18_5_isNormal() {
        assertEquals(BmiStatus.NORMAL, BmiCalculator.classify(18.5))
    }

    @Test
    fun classify_justBelow25_isNormal() {
        assertEquals(BmiStatus.NORMAL, BmiCalculator.classify(24.99))
    }

    @Test
    fun classify_exactly25_isOverweight() {
        assertEquals(BmiStatus.OVERWEIGHT, BmiCalculator.classify(25.0))
    }

    @Test
    fun classify_typicalValues() {
        assertEquals(BmiStatus.UNDERWEIGHT, BmiCalculator.classify(15.0))
        assertEquals(BmiStatus.NORMAL, BmiCalculator.classify(22.0))
        assertEquals(BmiStatus.OVERWEIGHT, BmiCalculator.classify(32.0))
    }

    @Test
    fun classify_rejectsInvalidBmi() {
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.classify(0.0) }
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.classify(Double.NaN) }
    }


    @Test
    fun heightAndWeight_atTheBoundaries_giveTheRightStatus() {
        assertEquals(BmiStatus.NORMAL, status(200.0, 74.0))       // 18.5
        assertEquals(BmiStatus.UNDERWEIGHT, status(200.0, 73.6))  // 18.4
        assertEquals(BmiStatus.NORMAL, status(200.0, 99.7))       // 24.9
        assertEquals(BmiStatus.OVERWEIGHT, status(200.0, 100.0))  // 25.0
    }


    @Test
    fun assessmentType_below25_isGeneral() {
        assertEquals(AssessmentType.GENERAL, AssessmentType.forBmi(24.9))
    }

    @Test
    fun assessmentType_exactly25_isOverweightForm() {
        assertEquals(AssessmentType.OVERWEIGHT, AssessmentType.forBmi(25.0))
    }

    private fun status(heightCm: Double, weightKg: Double): BmiStatus =
        BmiCalculator.classify(BmiCalculator.calculate(heightCm, weightKg))
}
