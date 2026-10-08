package com.example.patientvisits.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(
    val message: String? = null,
    val success: Boolean? = null,
    val code: Int? = null,
    val data: T? = null
)

@Serializable
data class Ack(val message: String? = null)

@Serializable
data class LoginData(
    @SerialName("access_token") val accessToken: String? = null
)

@Serializable
data class RemotePatient(
    val id: Int,
    val unique: String
)

@Serializable
data class VitalAck(
    val id: Int? = null,
    val message: String? = null
)

@Serializable
data class SignupRequest(
    val email: String,
    val firstname: String,
    val lastname: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class PatientRequest(
    val firstname: String,
    val lastname: String,
    val unique: String,
    val dob: String,
    val gender: String,
    @SerialName("reg_date") val regDate: String
)

@Serializable
data class VitalsRequest(
    @SerialName("visit_date") val visitDate: String,
    val height: String,
    val weight: String,
    val bmi: String,
    @SerialName("patient_id") val patientId: String
)

@Serializable
data class VisitsViewRequest(
    @SerialName("visit_date") val visitDate: String
)

@Serializable
data class VisitRequest(
    @SerialName("general_health") val generalHealth: String,
    @SerialName("on_diet") val onDiet: String? = null,
    @SerialName("on_drugs") val onDrugs: String? = null,
    val comments: String,
    @SerialName("visit_date") val visitDate: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("vital_id") val vitalId: String
)
