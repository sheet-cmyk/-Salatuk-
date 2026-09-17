package app.noor.prayer.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import app.noor.prayer.domain.LocationSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(@ApplicationContext private val context: Context, private val cities: OfflineCityLookup) {
    suspend fun current(): LocationSettings {
        val fine = ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        check(fine || ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) { "permission" }
        val token = CancellationTokenSource()
        try {
            val location = withTimeout(20_000) {
                LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token).await()
            } ?: error("unavailable")
            // Deliberately no network reverse geocoder: coordinates never leave this app.
            val labels = cities.nearest(location.latitude,location.longitude)
            return LocationSettings(location.latitude, location.longitude, labels.first, labels.second, automatic = true)
        } finally { token.cancel() }
    }
}
