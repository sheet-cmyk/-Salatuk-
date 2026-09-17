package app.noor.prayer.core.alarm

import android.content.Context
import androidx.work.*
import app.noor.prayer.di.AppEntryPoint
import app.noor.prayer.widget.PrayerWidget
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class MaintenanceWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result = try {
        EntryPointAccessors.fromApplication(applicationContext,AppEntryPoint::class.java).schedules().reschedule()
        PrayerWidget.refresh(applicationContext)
        Result.success()
    } catch (_: Exception) { Result.retry() }
    companion object {
        fun enqueue(context: Context) { WorkManager.getInstance(context).enqueueUniquePeriodicWork("prayer-maintenance",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<MaintenanceWorker>(12,TimeUnit.HOURS).build()) }
    }
}
