package com.example.patientvisits.data.fake

import com.example.patientvisits.domain.model.Assessment
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.model.PatientListItem
import com.example.patientvisits.domain.model.Vitals
import com.example.patientvisits.domain.repository.DuplicatePatientIdException
import com.example.patientvisits.domain.repository.DuplicateVisitException
import com.example.patientvisits.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.LocalDate


class InMemoryPatientRepository : PatientRepository {

    private val patients = MutableStateFlow<List<Patient>>(emptyList())
    private val vitals = MutableStateFlow<List<Vitals>>(emptyList())
    private val assessments = MutableStateFlow<List<Assessment>>(emptyList())

    override suspend fun isPatientIdTaken(patientId: String): Boolean =
        patients.value.any { it.patientId == patientId }

    override suspend fun registerPatient(patient: Patient) {
        if (isPatientIdTaken(patient.patientId)) throw DuplicatePatientIdException(patient.patientId)
        patients.update { it + patient }
    }

    override suspend fun getPatient(patientId: String): Patient? =
        patients.value.firstOrNull { it.patientId == patientId }

    override suspend fun hasVitalsOn(patientId: String, date: LocalDate): Boolean =
        vitals.value.any { it.patientId == patientId && it.visitDate == date }

    override suspend fun saveVitals(vitals: Vitals) {
        if (hasVitalsOn(vitals.patientId, vitals.visitDate)) {
            throw DuplicateVisitException("Vitals already recorded for this date")
        }
        this.vitals.update { it + vitals }
    }

    override suspend fun hasAssessmentOn(patientId: String, type: AssessmentType, date: LocalDate): Boolean =
        assessments.value.any { it.patientId == patientId && it.type == type && it.visitDate == date }

    override suspend fun saveAssessment(assessment: Assessment) {
        if (hasAssessmentOn(assessment.patientId, assessment.type, assessment.visitDate)) {
            throw DuplicateVisitException("Assessment already recorded for this date")
        }
        assessments.update { it + assessment }
    }

    override suspend fun syncPending(): Boolean = true

    override fun observePatientList(visitDate: LocalDate?): Flow<List<PatientListItem>> =
        combine(patients, vitals) { allPatients, allVitals ->
            allPatients.mapNotNull { patient ->
                val own = allVitals.filter { it.patientId == patient.patientId }
                val relevant =
                    if (visitDate == null) own.maxByOrNull { it.visitDate }
                    else own.firstOrNull { it.visitDate == visitDate }
                if (visitDate != null && relevant == null) null
                else PatientListItem(patient, relevant?.bmiStatus)
            }.sortedBy { it.patient.fullName.lowercase() }
        }
}
