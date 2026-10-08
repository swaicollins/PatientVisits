package com.example.patientvisits

import android.app.Application
import com.example.patientvisits.di.AppContainer

class PatientVisitsApp : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        container.syncScheduler.schedule()
    }
}
