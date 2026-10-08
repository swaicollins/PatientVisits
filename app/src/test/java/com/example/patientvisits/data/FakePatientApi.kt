package com.example.patientvisits.data

import com.example.patientvisits.data.remote.Ack
import com.example.patientvisits.data.remote.Envelope
import com.example.patientvisits.data.remote.LoginData
import com.example.patientvisits.data.remote.LoginRequest
import com.example.patientvisits.data.remote.PatientApi
import com.example.patientvisits.data.remote.PatientRequest
import com.example.patientvisits.data.remote.RemotePatient
import com.example.patientvisits.data.remote.SignupRequest
import com.example.patientvisits.data.remote.VisitRequest
import com.example.patientvisits.data.remote.VisitsViewRequest
import com.example.patientvisits.data.remote.VitalAck
import com.example.patientvisits.data.remote.VitalsRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

class FakePatientApi : PatientApi {

    val calls = mutableListOf<String>()
    val serverPatients = mutableListOf<RemotePatient>()
    val vitalsRequests = mutableListOf<VitalsRequest>()
    val visitRequests = mutableListOf<VisitRequest>()
    var registerCode = 200
    var loginCode = 200
    var signupCode = 200
    var networkDown = false
    val loginRequests = mutableListOf<LoginRequest>()
    val signupRequests = mutableListOf<SignupRequest>()
    var vitalsCode = 200
    var visitCode = 200
    private var nextPatientId = 1
    private var nextVitalId = 100

    private fun <T> reply(code: Int, data: T?): Response<Envelope<T>> =
        if (code in 200..299) {
            Response.success(Envelope(message = "success", success = true, code = code, data = data))
        } else {
            Response.error(code, "{}".toResponseBody("application/json".toMediaType()))
        }

    override suspend fun signup(body: SignupRequest): Response<Envelope<Ack>> {
        calls += "signup"
        if (networkDown) throw IOException("offline")
        signupRequests += body
        return reply(signupCode, Ack())
    }

    override suspend fun login(body: LoginRequest): Response<Envelope<LoginData>> {
        calls += "login"
        if (networkDown) throw IOException("offline")
        loginRequests += body
        return reply(loginCode, LoginData(accessToken = "token-abc"))
    }

    override suspend fun registerPatient(body: PatientRequest): Response<Envelope<Ack>> {
        calls += "register:${body.unique}"
        if (registerCode in 200..299) serverPatients += RemotePatient(nextPatientId++, body.unique)
        return reply(registerCode, Ack())
    }

    override suspend fun listPatients(): Response<Envelope<List<RemotePatient>>> =
        reply(200, serverPatients.toList())

    override suspend fun showPatient(id: Int): Response<ResponseBody> =
        Response.success("{}".toResponseBody())

    override suspend fun addVitals(body: VitalsRequest): Response<Envelope<VitalAck>> {
        calls += "vitals:${body.patientId}:${body.visitDate}"
        vitalsRequests += body
        return reply(vitalsCode, VitalAck(id = nextVitalId++))
    }

    override suspend fun viewVisits(body: VisitsViewRequest): Response<ResponseBody> =
        Response.success("{}".toResponseBody())

    override suspend fun addVisit(body: VisitRequest): Response<Envelope<Ack>> {
        calls += "visit:${body.patientId}:${body.visitDate}:${body.vitalId}"
        visitRequests += body
        return reply(visitCode, Ack())
    }
}
