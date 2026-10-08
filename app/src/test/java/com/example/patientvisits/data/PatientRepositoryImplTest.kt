package com.example.patientvisits.data

import com.example.patientvisits.data.sync.SyncScheduler
import com.example.patientvisits.domain.model.BmiStatus
import com.example.patientvisits.domain.model.Gender
import com.example.patientvisits.domain.model.GeneralAssessment
import com.example.patientvisits.domain.model.GeneralHealth
import com.example.patientvisits.domain.model.OverweightAssessment
import com.example.patientvisits.domain.model.Patient
import com.example.patientvisits.domain.model.Vitals
import com.example.patientvisits.domain.repository.DuplicatePatientIdException
import com.example.patientvisits.domain.repository.DuplicateVisitException
import kotlinx.coroutines.flow.first
import com.example.patientvisits.data.remote.RemotePatient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PatientRepositoryImplTest {

    private class FakeSession(var available: Boolean = true) : SessionProvider {
        var invalidated = 0
        override suspend fun ensureSession(): Boolean = available
        override suspend fun invalidate() {
            invalidated++
        }
    }

    private class CountingScheduler : SyncScheduler {
        var scheduled = 0
        override fun schedule() {
            scheduled++
        }
    }

    private lateinit var dao: FakePatientDao
    private lateinit var api: FakePatientApi
    private lateinit var session: FakeSession
    private lateinit var scheduler: CountingScheduler
    private lateinit var repository: PatientRepositoryImpl

    private val day1 = LocalDate.of(2026, 3, 1)
    private val day2 = LocalDate.of(2026, 3, 2)

    private val patient = Patient(
        patientId = "P-001",
        registrationDate = day1,
        firstName = "Jane",
        lastName = "Doe",
        dateOfBirth = LocalDate.of(1990, 5, 20),
        gender = Gender.FEMALE
    )

    @Before
    fun setUp() {
        dao = FakePatientDao()
        api = FakePatientApi()
        session = FakeSession()
        scheduler = CountingScheduler()
        repository = PatientRepositoryImpl(dao, api, session, scheduler)
    }

    @Test
    fun registerPatient_storesLocallyAndSchedulesSync() = runTest {
        repository.registerPatient(patient)

        assertEquals(patient, repository.getPatient("P-001"))
        assertEquals(1, scheduler.scheduled)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun registerPatient_withTakenId_isRejected() = runTest {
        repository.registerPatient(patient)

        try {
            repository.registerPatient(patient.copy(firstName = "Other"))
            fail("expected DuplicatePatientIdException")
        } catch (e: DuplicatePatientIdException) {
            assertEquals("P-001", e.patientId)
        }
        assertEquals(1, dao.patients.value.size)
    }

    @Test
    fun saveVitals_onSameDate_isRejectedButNextDateIsAccepted() = runTest {
        repository.registerPatient(patient)
        repository.saveVitals(Vitals.create("P-001", day1, 170.0, 70.0))

        try {
            repository.saveVitals(Vitals.create("P-001", day1, 171.0, 71.0))
            fail("expected DuplicateVisitException")
        } catch (e: DuplicateVisitException) {
        }

        repository.saveVitals(Vitals.create("P-001", day2, 170.0, 70.0))
        assertEquals(2, dao.vitals.value.size)
    }

    @Test
    fun saveAssessment_sameDateAllowedAcrossTypesButNotWithinOne() = runTest {
        repository.registerPatient(patient)
        val general = GeneralAssessment("P-001", day1, GeneralHealth.GOOD, true, "ok")
        val overweight = OverweightAssessment("P-001", day1, GeneralHealth.POOR, false, "ok")

        repository.saveAssessment(general)
        repository.saveAssessment(overweight)

        try {
            repository.saveAssessment(general)
            fail("expected DuplicateVisitException")
        } catch (e: DuplicateVisitException) {
        }
        assertEquals(2, dao.assessments.value.size)
    }

    @Test
    fun syncPending_pushesPatientBeforeDependentRecordsAndLinksServerIds() = runTest {
        repository.registerPatient(patient)
        repository.saveVitals(Vitals.create("P-001", day1, 170.0, 80.0))
        repository.saveAssessment(OverweightAssessment("P-001", day1, GeneralHealth.GOOD, false, "fine"))

        val result = repository.syncPending()

        assertTrue(result)
        assertEquals(
            listOf("register:P-001", "vitals:1:2026-03-01", "visit:1:2026-03-01:100"),
            api.calls
        )
        val visit = api.visitRequests.single()
        assertEquals("Good", visit.generalHealth)
        assertEquals("No", visit.onDrugs)
        assertNull(visit.onDiet)
        assertEquals("80.0", api.vitalsRequests.single().weight)
        assertEquals(1, dao.patients.value.single().remoteId)
        assertEquals(100, dao.vitals.value.single().remoteId)
        assertTrue(dao.patients.value.all { it.synced })
        assertTrue(dao.vitals.value.all { it.synced })
        assertTrue(dao.assessments.value.all { it.synced })
    }

    @Test
    fun syncPending_sendsGeneralFormWithDietAnswer() = runTest {
        repository.registerPatient(patient)
        repository.saveVitals(Vitals.create("P-001", day1, 170.0, 60.0))
        repository.saveAssessment(GeneralAssessment("P-001", day1, GeneralHealth.POOR, true, "ok"))

        assertTrue(repository.syncPending())

        val visit = api.visitRequests.single()
        assertEquals("Poor", visit.generalHealth)
        assertEquals("Yes", visit.onDiet)
        assertNull(visit.onDrugs)
    }

    @Test
    fun syncPending_whenPatientPushFails_holdsBackDependentRecords() = runTest {
        api.registerCode = 500
        repository.registerPatient(patient)
        repository.saveVitals(Vitals.create("P-001", day1, 170.0, 60.0))
        repository.saveAssessment(GeneralAssessment("P-001", day1, GeneralHealth.GOOD, true, "ok"))

        val result = repository.syncPending()

        assertFalse(result)
        assertEquals(listOf("register:P-001"), api.calls)
        assertTrue(dao.patients.value.none { it.synced })
        assertTrue(dao.vitals.value.none { it.synced })
        assertTrue(dao.assessments.value.none { it.synced })
    }

    @Test
    fun syncPending_isRetryableAfterFailureWithoutReRegistering() = runTest {
        api.vitalsCode = 503
        repository.registerPatient(patient)
        repository.saveVitals(Vitals.create("P-001", day1, 170.0, 60.0))

        assertFalse(repository.syncPending())
        assertTrue(dao.patients.value.single().synced)
        assertFalse(dao.vitals.value.single().synced)

        api.vitalsCode = 200
        assertTrue(repository.syncPending())
        assertTrue(dao.vitals.value.single().synced)
        assertEquals(1, api.calls.count { it.startsWith("register:") })
    }

    @Test
    fun syncPending_reusesPatientAlreadyOnServer() = runTest {
        api.serverPatients += RemotePatient(7, "P-001")
        repository.registerPatient(patient)

        assertTrue(repository.syncPending())
        assertTrue(api.calls.none { it.startsWith("register:") })
        assertEquals(7, dao.patients.value.single().remoteId)
    }

    @Test
    fun syncPending_withoutSession_doesNotCallTheApi() = runTest {
        session.available = false
        repository.registerPatient(patient)

        assertFalse(repository.syncPending())
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun syncPending_onUnauthorized_invalidatesSession() = runTest {
        api.registerCode = 401
        repository.registerPatient(patient)

        assertFalse(repository.syncPending())
        assertEquals(1, session.invalidated)
    }

    @Test
    fun patientList_showsLatestBmiStatusAndFiltersByVisitDate() = runTest {
        repository.registerPatient(patient)
        repository.registerPatient(patient.copy(patientId = "P-002", firstName = "Adam"))
        repository.saveVitals(Vitals.create("P-001", day1, 170.0, 80.0))
        repository.saveVitals(Vitals.create("P-001", day2, 170.0, 60.0))

        val all = repository.observePatientList(null).first()
        assertEquals(listOf("Adam Doe", "Jane Doe"), all.map { it.patient.fullName })
        assertNull(all[0].bmiStatus)
        assertEquals(BmiStatus.NORMAL, all[1].bmiStatus)

        val onDay1 = repository.observePatientList(day1).first()
        assertEquals(1, onDay1.size)
        assertEquals(BmiStatus.OVERWEIGHT, onDay1.single().bmiStatus)

        assertTrue(repository.observePatientList(LocalDate.of(2026, 3, 9)).first().isEmpty())
    }
}
