package com.example.patientvisits.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatientDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PatientDao

    private val patient = PatientEntity(
        patientId = "P-001",
        registrationDate = "2026-03-01",
        firstName = "Jane",
        lastName = "Doe",
        dateOfBirth = "1990-05-20",
        gender = "FEMALE"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.patientDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertedPatient_canBeFoundAndObserved() = runBlocking {
        dao.insertPatient(patient)

        assertEquals(patient, dao.findPatient("P-001"))
        assertNull(dao.findPatient("missing"))
        assertEquals(1, dao.observePatients().first().size)
    }

    @Test
    fun duplicatePatientId_isRejectedByThePrimaryKey() = runBlocking {
        dao.insertPatient(patient)

        try {
            dao.insertPatient(patient.copy(firstName = "Other"))
            fail("expected SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            assertEquals(1, dao.countPatients("P-001"))
        }
    }

    @Test
    fun secondVitalsOnTheSameDate_isRejectedByTheUniqueIndex() = runBlocking {
        dao.insertPatient(patient)
        val vitals = VitalsEntity(
            patientId = "P-001",
            visitDate = "2026-03-01",
            heightCm = 170.0,
            weightKg = 70.0,
            bmi = 24.2
        )
        dao.insertVitals(vitals)

        try {
            dao.insertVitals(vitals.copy(weightKg = 71.0))
            fail("expected SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            assertEquals(1, dao.countVitals("P-001", "2026-03-01"))
        }

        dao.insertVitals(vitals.copy(visitDate = "2026-03-02"))
        assertEquals(2, dao.observeVitals().first().size)
    }

    @Test
    fun vitalsForUnknownPatient_isRejectedByTheForeignKey() = runBlocking {
        try {
            dao.insertVitals(
                VitalsEntity(
                    patientId = "ghost",
                    visitDate = "2026-03-01",
                    heightCm = 170.0,
                    weightKg = 70.0,
                    bmi = 24.2
                )
            )
            fail("expected SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
        }
    }

    @Test
    fun markingRecordsSynced_removesThemFromTheUnsyncedQueries() = runBlocking {
        dao.insertPatient(patient)
        val vitalsId = dao.insertVitals(
            VitalsEntity(
                patientId = "P-001",
                visitDate = "2026-03-01",
                heightCm = 170.0,
                weightKg = 70.0,
                bmi = 24.2
            )
        )
        val assessmentId = dao.insertAssessment(
            AssessmentEntity(
                patientId = "P-001",
                type = "GENERAL",
                visitDate = "2026-03-01",
                generalHealth = "GOOD",
                answer = true,
                comments = "ok"
            )
        )
        assertEquals(1, dao.unsyncedPatients().size)
        assertEquals(1, dao.unsyncedVitals().size)
        assertEquals(1, dao.unsyncedAssessments().size)

        dao.markPatientSynced("P-001", 3)
        dao.markVitalsSynced(vitalsId, 9)
        dao.markAssessmentSynced(assessmentId)

        assertTrue(dao.unsyncedPatients().isEmpty())
        assertTrue(dao.unsyncedVitals().isEmpty())
        assertTrue(dao.unsyncedAssessments().isEmpty())
        assertEquals(3, dao.findPatient("P-001")?.remoteId)
        assertEquals(9, dao.latestVitalsFor("P-001", "2026-03-01")?.remoteId)
    }

    @Test
    fun latestVitalsFor_prefersTheMatchingDateThenTheNewest() = runBlocking {
        dao.insertPatient(patient)
        val base = VitalsEntity(
            patientId = "P-001",
            visitDate = "2026-03-01",
            heightCm = 170.0,
            weightKg = 70.0,
            bmi = 24.2
        )
        dao.insertVitals(base)
        dao.insertVitals(base.copy(visitDate = "2026-03-05"))

        assertEquals("2026-03-01", dao.latestVitalsFor("P-001", "2026-03-01")?.visitDate)
        assertEquals("2026-03-05", dao.latestVitalsFor("P-001", "2026-03-03")?.visitDate)
        assertNull(dao.latestVitalsFor("other", "2026-03-01"))
    }

    @Test
    fun sameDateAssessments_areAllowedAcrossTypesOnly() = runBlocking {
        dao.insertPatient(patient)
        val general = AssessmentEntity(
            patientId = "P-001",
            type = "GENERAL",
            visitDate = "2026-03-01",
            generalHealth = "GOOD",
            answer = true,
            comments = "ok"
        )
        dao.insertAssessment(general)
        dao.insertAssessment(general.copy(type = "OVERWEIGHT"))

        try {
            dao.insertAssessment(general)
            fail("expected SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
        }
        assertEquals(1, dao.countAssessments("P-001", "GENERAL", "2026-03-01"))
        assertEquals(1, dao.countAssessments("P-001", "OVERWEIGHT", "2026-03-01"))
    }
}
