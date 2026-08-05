package com.vpsguardian.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vpsguardian.app.domain.repository.VpsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val vpsRepository: VpsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            vpsRepository.syncAllVps()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
