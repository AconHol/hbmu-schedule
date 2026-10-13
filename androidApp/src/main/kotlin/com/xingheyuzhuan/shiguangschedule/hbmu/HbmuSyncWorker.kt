package com.xingheyuzhuan.shiguangschedule.hbmu

import android.content.Context
import androidx.work.*
import org.koin.android.annotation.KoinWorker
import java.util.concurrent.TimeUnit

@KoinWorker
class HbmuSyncWorker(context: Context, params: WorkerParameters, private val repository: HbmuRepository) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        repository.resume()
        return if (repository.uiState.value.error == null) Result.success() else Result.failure()
    }
}

fun scheduleHbmuSync(context: Context) {
    val request = PeriodicWorkRequestBuilder<HbmuSyncWorker>(6, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("hbmu-course-sync", ExistingPeriodicWorkPolicy.KEEP, request)
}
