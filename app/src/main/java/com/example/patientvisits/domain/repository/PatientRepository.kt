package com.example.patientvisits.domain.repository

import com.example.patientvisits.domain.model.Assessment
import com.example.patientvisits.domain.model.AssessmentType
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.model.PatientListItem
import com.example.patientvisits.domain.model.Vitals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DuplicatePatientIdException(val patientId: String) : Exception("Patient $patientId already exists")
class DuplicateVisitException(message: String) : Exception(message)


interface PatientRepository {

    suspend fun isPatientIdTaken(patientId: String): Boolean

    suspend fun registerPatient(patient: Patient)

    suspend fun getPatient(patientId: String): Patient?

    suspend fun hasVitalsOn(patientId: String, date: LocalDate): Boolean

    suspend fun saveVitals(vitals: Vitals)

    suspend fun hasAssessmentOn(patientId: String, type: AssessmentType, date: LocalDate): Boolean

    suspend fun saveAssessment(assessment: Assessment)


    fun observePatientList(visitDate: LocalDate?): Flow<List<PatientListItem>>

    suspend fun syncPending(): Boolean
}
