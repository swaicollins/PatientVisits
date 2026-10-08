package com.example.patientvisits.domain

import com.example.patientvisits.domain.model.BmiStatus
import java.math.BigDecimal
import java.math.RoundingMode


object BmiCalculator {

    const val UNDERWEIGHT_BELOW = 18.5

    const val OVERWEIGHT_FROM = 25.0


    fun calculate(heightCm: Double, weightKg: Double): Double {
        require(heightCm.isFinite() && heightCm > 0) { "Height must be greater than 0 cm" }
        require(weightKg.isFinite() && weightKg > 0) { "Weight must be greater than 0 kg" }

        val heightM = heightCm / 100.0
        val raw = weightKg / (heightM * heightM)

        return BigDecimal.valueOf(raw).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    fun classify(bmi: Double): BmiStatus {
        require(bmi.isFinite() && bmi > 0) { "BMI must be a positive number" }
        return when {
            bmi < UNDERWEIGHT_BELOW -> BmiStatus.UNDERWEIGHT
            bmi < OVERWEIGHT_FROM -> BmiStatus.NORMAL
            else -> BmiStatus.OVERWEIGHT
        }
    }
}
