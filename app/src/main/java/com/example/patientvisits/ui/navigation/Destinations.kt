package com.example.patientvisits.ui.navigation

import com.example.patientvisits.domain.model.AssessmentType
import kotlinx.serialization.Serializable


@Serializable
data object LoginDestination

@Serializable
data object SignupDestination

@Serializable
data object ListingDestination

@Serializable
data object RegistrationDestination

@Serializable
data class VitalsDestination(val patientId: String)

@Serializable
data class AssessmentDestination(
    val patientId: String,
    val type: AssessmentType,
    val visitDate: String
)
