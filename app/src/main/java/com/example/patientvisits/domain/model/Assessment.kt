package com.example.patientvisits.domain.model

import com.example.patientvisits.domain.BmiCalculator
import java.time.LocalDate

enum class GeneralHealth { GOOD, POOR }

enum class AssessmentType {
    GENERAL,

    OVERWEIGHT;

    companion object {
        fun forBmi(bmi: Double): AssessmentType =
            if (bmi < BmiCalculator.OVERWEIGHT_FROM) GENERAL else OVERWEIGHT
    }
}


sealed interface Assessment {
    val patientId: String
    val visitDate: LocalDate
    val generalHealth: GeneralHealth
    val comments: String
    val type: AssessmentType
}

data class GeneralAssessment(
    override val patientId: String,
    override val visitDate: LocalDate,
    override val generalHealth: GeneralHealth,
    val everOnDiet: Boolean,
    override val comments: String
) : Assessment {
    override val type: AssessmentType get() = AssessmentType.GENERAL
}

data class OverweightAssessment(
    override val patientId: String,
    override val visitDate: LocalDate,
    override val generalHealth: GeneralHealth,
    val currentlyOnDrugs: Boolean,
    override val comments: String
) : Assessment {
    override val type: AssessmentType get() = AssessmentType.OVERWEIGHT
}
