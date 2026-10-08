package com.example.patientvisits.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: PatientEntity)

    @Query("SELECT COUNT(*) FROM patients WHERE patientId = :patientId")
    suspend fun countPatients(patientId: String): Int

    @Query("SELECT * FROM patients WHERE patientId = :patientId")
    suspend fun findPatient(patientId: String): PatientEntity?

    @Query("SELECT * FROM patients")
    fun observePatients(): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVitals(vitals: VitalsEntity): Long

    @Query("SELECT COUNT(*) FROM vitals WHERE patientId = :patientId AND visitDate = :visitDate")
    suspend fun countVitals(patientId: String, visitDate: String): Int

    @Query("SELECT * FROM vitals")
    fun observeVitals(): Flow<List<VitalsEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Query(
        "SELECT COUNT(*) FROM assessments " +
            "WHERE patientId = :patientId AND type = :type AND visitDate = :visitDate"
    )
    suspend fun countAssessments(patientId: String, type: String, visitDate: String): Int

    @Query("SELECT * FROM patients WHERE synced = 0")
    suspend fun unsyncedPatients(): List<PatientEntity>

    @Query("SELECT * FROM vitals WHERE synced = 0")
    suspend fun unsyncedVitals(): List<VitalsEntity>

    @Query("SELECT * FROM assessments WHERE synced = 0")
    suspend fun unsyncedAssessments(): List<AssessmentEntity>

    @Query("SELECT * FROM vitals WHERE patientId = :patientId ORDER BY (visitDate = :visitDate) DESC, visitDate DESC LIMIT 1")
    suspend fun latestVitalsFor(patientId: String, visitDate: String): VitalsEntity?

    @Query("UPDATE patients SET synced = 1, remoteId = :remoteId WHERE patientId = :patientId")
    suspend fun markPatientSynced(patientId: String, remoteId: Int)

    @Query("UPDATE vitals SET synced = 1, remoteId = :remoteId WHERE id = :id")
    suspend fun markVitalsSynced(id: Long, remoteId: Int)

    @Query("UPDATE assessments SET synced = 1 WHERE id = :id")
    suspend fun markAssessmentSynced(id: Long)
}
