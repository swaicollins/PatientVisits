package com.example.patientvisits.domain.model

import java.time.LocalDate
import java.time.Period

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}


data class Patient(
    val patientId: String,
    val registrationDate: LocalDate,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender
) {
    val fullName: String get() = "$firstName $lastName"

    fun ageOn(today: LocalDate): Int = Period.between(dateOfBirth, today).years
}
