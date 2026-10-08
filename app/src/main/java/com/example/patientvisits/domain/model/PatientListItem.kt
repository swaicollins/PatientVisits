package com.example.patientvisits.domain.model


data class PatientListItem(
    val patient: Patient,
    val bmiStatus: BmiStatus?
)
