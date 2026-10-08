package com.example.patientvisits.data

import com.example.patientvisits.data.local.AssessmentEntity
import com.example.patientvisits.data.local.PatientDao
import com.example.patientvisits.data.local.PatientEntity
import com.example.patientvisits.data.local.VitalsEntity
import com.example.patientvisits.data.local.toDomain
import com.example.patientvisits.data.local.toEntity
import com.example.patientvisits.data.remote.Envelope
import com.example.patientvisits.data.remote.PatientApi
import com.example.patientvisits.data.remote.PatientRequest
import com.example.patientvisits.data.remote.RemotePatient
import com.example.patientvisits.data.remote.VisitRequest
import com.example.patientvisits.data.remote.VitalsRequest
import com.example.patientvisits.data.sync.SyncScheduler
import com.example.patientvisits.domain.model.Assessment
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.Gender
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.model.PatientListItem
import com.example.patientvisits.domain.model.Vitals
import com.example.patientvisits.domain.repository.DuplicatePatientIdException
import com.example.patientvisits.domain.repository.DuplicateVisitException
import com.example.patientvisits.domain.repository.PatientRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import retrofit2.Response
import java.time.LocalDate

class PatientRepositoryImpl(
    private val dao: PatientDao,
    private val api: PatientApi,
    private val session: SessionProvider,
    private val scheduler: SyncScheduler
) : PatientRepository {

    override suspend fun isPatientIdTaken(patientId: String): Boolean =
        dao.countPatients(patientId) > 0

    override suspend fun registerPatient(patient: Patient) {
        if (isPatientIdTaken(patient.patientId)) throw DuplicatePatientIdException(patient.patientId)
        dao.insertPatient(patient.toEntity())
        scheduler.schedule()
    }

    override suspend fun getPatient(patientId: String): Patient? =
        dao.findPatient(patientId)?.toDomain()

    override suspend fun hasVitalsOn(patientId: String, date: LocalDate): Boolean =
        dao.countVitals(patientId, date.toString()) > 0

    override suspend fun saveVitals(vitals: Vitals) {
        if (hasVitalsOn(vitals.patientId, vitals.visitDate)) {
            throw DuplicateVisitException("Vitals already recorded for this date")
        }
        dao.insertVitals(vitals.toEntity())
        scheduler.schedule()
    }

    override suspend fun hasAssessmentOn(patientId: String, type: AssessmentType, date: LocalDate): Boolean =
        dao.countAssessments(patientId, type.name, date.toString()) > 0

    override suspend fun saveAssessment(assessment: Assessment) {
        if (hasAssessmentOn(assessment.patientId, assessment.type, assessment.visitDate)) {
            throw DuplicateVisitException("Assessment already recorded for this date")
        }
        dao.insertAssessment(assessment.toEntity())
        scheduler.schedule()
    }

    override fun observePatientList(visitDate: LocalDate?): Flow<List<PatientListItem>> =
        combine(dao.observePatients(), dao.observeVitals()) { patients, vitals ->
            val byPatient = vitals.map { it.toDomain() }.groupBy { it.patientId }
            patients.mapNotNull { entity ->
                val patient = entity.toDomain()
                val own = byPatient[patient.patientId].orEmpty()
                val relevant =
                    if (visitDate == null) own.maxByOrNull { it.visitDate }
                    else own.firstOrNull { it.visitDate == visitDate }
                if (visitDate != null && relevant == null) null
                else PatientListItem(patient, relevant?.bmiStatus)
            }.sortedBy { it.patient.fullName.lowercase() }
        }

    override suspend fun syncPending(): Boolean {
        if (!session.ensureSession()) return false
        var allSynced = true
        var remotePatients: List<RemotePatient>? = null

        suspend fun remoteId(unique: String): Int? {
            if (remotePatients == null) {
                remotePatients = push { api.listPatients() }?.data
            }
            return remotePatients?.firstOrNull { it.unique == unique }?.id
        }

        for (patient in dao.unsyncedPatients()) {
            var id = remoteId(patient.patientId)
            if (id == null) {
                push { api.registerPatient(patient.toRequest()) }
                remotePatients = null
                id = remoteId(patient.patientId)
            }
            if (id != null) dao.markPatientSynced(patient.patientId, id) else allSynced = false
        }

        for (vitals in dao.unsyncedVitals()) {
            val patientRemoteId = dao.findPatient(vitals.patientId)?.remoteId
            if (patientRemoteId == null) {
                allSynced = false
                continue
            }
            val vitalRemoteId = push { api.addVitals(vitals.toRequest(patientRemoteId)) }?.data?.id
            if (vitalRemoteId != null) dao.markVitalsSynced(vitals.id, vitalRemoteId) else allSynced = false
        }

        for (assessment in dao.unsyncedAssessments()) {
            val patientRemoteId = dao.findPatient(assessment.patientId)?.remoteId
            val vitalRemoteId = dao.latestVitalsFor(assessment.patientId, assessment.visitDate)?.remoteId
            if (patientRemoteId == null || vitalRemoteId == null) {
                allSynced = false
                continue
            }
            val delivered = push { api.addVisit(assessment.toRequest(patientRemoteId, vitalRemoteId)) }
            if (delivered != null) dao.markAssessmentSynced(assessment.id) else allSynced = false
        }

        return allSynced
    }

    private suspend fun <T> push(call: suspend () -> Response<Envelope<T>>): Envelope<T>? = try {
        val response = call()
        val body = response.body()
        when {
            response.isSuccessful && body != null && body.success != false -> body
            response.code() == HTTP_UNAUTHORIZED -> {
                session.invalidate()
                null
            }
            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun PatientEntity.toRequest() = PatientRequest(
        firstname = firstName,
        lastname = lastName,
        unique = patientId,
        dob = dateOfBirth,
        gender = Gender.valueOf(gender).label,
        regDate = registrationDate
    )

    private fun VitalsEntity.toRequest(patientRemoteId: Int) = VitalsRequest(
        visitDate = visitDate,
        height = heightCm.toString(),
        weight = weightKg.toString(),
        bmi = bmi.toString(),
        patientId = patientRemoteId.toString()
    )

    private fun AssessmentEntity.toRequest(patientRemoteId: Int, vitalRemoteId: Int): VisitRequest {
        val general = type == AssessmentType.GENERAL.name
        val answer = if (this.answer) "Yes" else "No"
        return VisitRequest(
            generalHealth = generalHealth.lowercase().replaceFirstChar { it.uppercase() },
            onDiet = if (general) answer else null,
            onDrugs = if (general) null else answer,
            comments = comments,
            visitDate = visitDate,
            patientId = patientRemoteId.toString(),
            vitalId = vitalRemoteId.toString()
        )
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
