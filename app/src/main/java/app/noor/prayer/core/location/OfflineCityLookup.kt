package app.noor.prayer.core.location

import android.content.Context
import android.location.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineCityLookup @Inject constructor(@ApplicationContext private val context: Context) {
    private data class City(val name: String,val country: String,val lat: Double,val lon: Double)
    private val cities: List<City> by lazy {
        context.assets.open("cities.tsv").bufferedReader().useLines { lines -> lines.mapNotNull { line ->
            val c = line.split('\t'); runCatching { City(c[0],c[1],c[2].toDouble(),c[3].toDouble()) }.getOrNull()
        }.toList() }
    }
    suspend fun nearest(latitude: Double,longitude: Double): Pair<String,String> = withContext(Dispatchers.Default) {
        val distance = FloatArray(1)
        var nearest: City? = null; var best = 50_000f
        cities.asSequence().filter { kotlin.math.abs(it.lat-latitude) < 1.0 }.forEach { city ->
            Location.distanceBetween(latitude,longitude,city.lat,city.lon,distance)
            if(distance[0] < best) { best = distance[0]; nearest = city }
        }
        nearest?.let { it.name to Locale.Builder().setRegion(it.country).build().displayCountry } ?: ("" to "")
    }
}
