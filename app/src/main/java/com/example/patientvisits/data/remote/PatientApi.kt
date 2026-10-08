package com.example.patientvisits.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PatientApi {

    @POST("user/signup")
    suspend fun signup(@Body body: SignupRequest): Response<Envelope<Ack>>

    @POST("user/signin")
    suspend fun login(@Body body: LoginRequest): Response<Envelope<LoginData>>

    @POST("patients/register")
    suspend fun registerPatient(@Body body: PatientRequest): Response<Envelope<Ack>>

    @GET("patients/view")
    suspend fun listPatients(): Response<Envelope<List<RemotePatient>>>

    @GET("patients/show/{id}")
    suspend fun showPatient(@Path("id") id: Int): Response<ResponseBody>

    @POST("vital/add")
    suspend fun addVitals(@Body body: VitalsRequest): Response<Envelope<VitalAck>>

    @POST("visits/view")
    suspend fun viewVisits(@Body body: VisitsViewRequest): Response<ResponseBody>

    @POST("visits/add")
    suspend fun addVisit(@Body body: VisitRequest): Response<Envelope<Ack>>
}
