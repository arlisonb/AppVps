package com.vpsguardian.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vpsguardian.app.data.monitor.VpsMonitor
import com.vpsguardian.app.data.session.CredentialsStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val credentialsStore: CredentialsStore,
    private val vpsMonitor: VpsMonitor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val credentials = credentialsStore.load() ?: return Result.success()
        return vpsMonitor.runCheck(credentials, notify = true)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { if (runAttemptCount < 2) Result.retry() else Result.failure() }
            )
    }
}
