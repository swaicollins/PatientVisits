package com.example.patientvisits.domain.model

import com.example.patientvisits.domain.BmiCalculator
import java.time.LocalDate


data class Vitals(
    val patientId: String,
    val visitDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val bmi: Double
) {
    val bmiStatus: BmiStatus get() = BmiCalculator.classify(bmi)

    companion object {
        fun create(patientId: String, visitDate: LocalDate, heightCm: Double, weightKg: Double) =
            Vitals(
                patientId = patientId,
                visitDate = visitDate,
                heightCm = heightCm,
                weightKg = weightKg,
                bmi = BmiCalculator.calculate(heightCm, weightKg)
            )
    }
}
