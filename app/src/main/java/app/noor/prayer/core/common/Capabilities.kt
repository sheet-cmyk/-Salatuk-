package app.noor.prayer.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.noor.prayer.core.alarm.PrayerAlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class Capabilities(val exact: Boolean = false, val notifications: Boolean = false, val location: Boolean = false, val batteryUnrestricted: Boolean = false, val alarmVolumeOk: Boolean = true)
class CapabilityRepository @Inject constructor(@ApplicationContext private val context: Context, private val scheduler: PrayerAlarmScheduler) {
    fun read(): Capabilities {
        val power = context.getSystemService(PowerManager::class.java)
        val audio = context.getSystemService(AudioManager::class.java)
        return Capabilities(
            scheduler.allowed(),
            NotificationManagerCompat.from(context).areNotificationsEnabled(),
            ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED,
            power?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
            (audio?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 1) > 0,
        )
    }
}
