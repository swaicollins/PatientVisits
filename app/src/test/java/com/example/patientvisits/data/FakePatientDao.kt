package com.example.patientvisits.data

import com.example.patientvisits.data.local.AssessmentEntity
import com.example.patientvisits.data.local.PatientDao
import com.example.patientvisits.data.local.PatientEntity
import com.example.patientvisits.data.local.VitalsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePatientDao : PatientDao {

    val patients = MutableStateFlow<List<PatientEntity>>(emptyList())
    val vitals = MutableStateFlow<List<VitalsEntity>>(emptyList())
    val assessments = MutableStateFlow<List<AssessmentEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insertPatient(patient: PatientEntity) {
        check(patients.value.none { it.patientId == patient.patientId }) { "unique violation" }
        patients.value = patients.value + patient
    }

    override suspend fun countPatients(patientId: String): Int =
        patients.value.count { it.patientId == patientId }

    override suspend fun findPatient(patientId: String): PatientEntity? =
        patients.value.firstOrNull { it.patientId == patientId }

    override fun observePatients(): Flow<List<PatientEntity>> = patients

    override suspend fun insertVitals(vitals: VitalsEntity): Long {
        val stored = vitals.copy(id = nextId++)
        this.vitals.value = this.vitals.value + stored
        return stored.id
    }

    override suspend fun countVitals(patientId: String, visitDate: String): Int =
        vitals.value.count { it.patientId == patientId && it.visitDate == visitDate }

    override fun observeVitals(): Flow<List<VitalsEntity>> = vitals

    override suspend fun insertAssessment(assessment: AssessmentEntity): Long {
        val stored = assessment.copy(id = nextId++)
        assessments.value = assessments.value + stored
        return stored.id
    }

    override suspend fun countAssessments(patientId: String, type: String, visitDate: String): Int =
        assessments.value.count { it.patientId == patientId && it.type == type && it.visitDate == visitDate }

    override suspend fun unsyncedPatients(): List<PatientEntity> = patients.value.filter { !it.synced }

    override suspend fun unsyncedVitals(): List<VitalsEntity> = vitals.value.filter { !it.synced }

    override suspend fun unsyncedAssessments(): List<AssessmentEntity> = assessments.value.filter { !it.synced }

    override suspend fun latestVitalsFor(patientId: String, visitDate: String): VitalsEntity? =
        vitals.value.filter { it.patientId == patientId }
            .sortedWith(compareByDescending<VitalsEntity> { it.visitDate == visitDate }.thenByDescending { it.visitDate })
            .firstOrNull()

    override suspend fun markPatientSynced(patientId: String, remoteId: Int) {
        patients.value = patients.value.map {
            if (it.patientId == patientId) it.copy(synced = true, remoteId = remoteId) else it
        }
    }

    override suspend fun markVitalsSynced(id: Long, remoteId: Int) {
        vitals.value = vitals.value.map { if (it.id == id) it.copy(synced = true, remoteId = remoteId) else it }
    }

    override suspend fun markAssessmentSynced(id: Long) {
        assessments.value = assessments.value.map { if (it.id == id) it.copy(synced = true) else it }
    }
}
