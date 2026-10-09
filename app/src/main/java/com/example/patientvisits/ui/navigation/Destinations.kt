package com.example.patientvisits.ui.navigation

import com.example.patientvisits.domain.model.AssessmentType
import kotlinx.serialization.Serializable


/** [email] pre-fills the field and [accountCreated] shows the "account created" notice after sign-up. */
@Serializable
data class LoginDestination(
    val email: String = "",
    val accountCreated: Boolean = false
)

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
