package com.example.patientvisits.data.local

import com.example.patientvisits.domain.model.Assessment
import com.example.patientvisits.domain.model.Gender
import com.example.patientvisits.domain.model.GeneralAssessment
import com.example.patientvisits.domain.model.OverweightAssessment
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.model.Vitals
import java.time.LocalDate

fun Patient.toEntity() = PatientEntity(
    patientId = patientId,
    registrationDate = registrationDate.toString(),
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth.toString(),
    gender = gender.name
)

fun PatientEntity.toDomain() = Patient(
    patientId = patientId,
    registrationDate = LocalDate.parse(registrationDate),
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = LocalDate.parse(dateOfBirth),
    gender = Gender.valueOf(gender)
)

fun Vitals.toEntity() = VitalsEntity(
    patientId = patientId,
    visitDate = visitDate.toString(),
    heightCm = heightCm,
    weightKg = weightKg,
    bmi = bmi
)

fun VitalsEntity.toDomain() = Vitals(
    patientId = patientId,
    visitDate = LocalDate.parse(visitDate),
    heightCm = heightCm,
    weightKg = weightKg,
    bmi = bmi
)

fun Assessment.toEntity() = AssessmentEntity(
    patientId = patientId,
    type = type.name,
    visitDate = visitDate.toString(),
    generalHealth = generalHealth.name,
    answer = when (this) {
        is GeneralAssessment -> everOnDiet
        is OverweightAssessment -> currentlyOnDrugs
    },
    comments = comments
)
