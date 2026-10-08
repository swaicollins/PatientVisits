package com.example.patientvisits.di

import android.content.Context
import com.example.patientvisits.data.DataStoreTokenStore
import com.example.patientvisits.data.PatientRepositoryImpl
import com.example.patientvisits.data.SessionManager
import com.example.patientvisits.data.local.AppDatabase
import com.example.patientvisits.data.remote.ApiFactory
import com.example.patientvisits.data.remote.PatientApi
import com.example.patientvisits.data.sync.SyncScheduler
import com.example.patientvisits.data.sync.WorkManagerSyncScheduler
import com.example.patientvisits.domain.repository.AuthRepository
import com.example.patientvisits.domain.repository.PatientRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    private val tokenStore by lazy { DataStoreTokenStore(appContext) }
    private val api: PatientApi by lazy { ApiFactory.create(tokenStore) }
    private val session by lazy { SessionManager(api, tokenStore) }

    val authRepository: AuthRepository by lazy { session }

    val syncScheduler: SyncScheduler by lazy { WorkManagerSyncScheduler(appContext) }

    val repository: PatientRepository by lazy {
        PatientRepositoryImpl(database.patientDao(), api, session, syncScheduler)
    }
}
