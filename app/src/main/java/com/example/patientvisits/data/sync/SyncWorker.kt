package com.example.patientvisits.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.patientvisits.PatientVisitsApp
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PatientVisitsApp).container
        if (!container.authRepository.isLoggedIn.first()) return Result.success()
        return if (container.repository.syncPending()) Result.success() else Result.retry()
    }
}
