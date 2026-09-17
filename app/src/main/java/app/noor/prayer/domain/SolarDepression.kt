package app.noor.prayer.domain

import java.time.Instant
import kotlin.math.*

/** NOAA/Meeus solar-position equations, used only for Tehran's 4.5-degree Maghrib.
 * Adhan Kotlin exposes Fajr/Isha angles but no Maghrib angle parameter.
 * Reference: https://gml.noaa.gov/grad/solcalc/calcdetails.html
 */
internal object SolarDepression {
    fun afterSunset(sunset: Instant, location: LocationSettings, depression: Double): Instant {
        var lo = sunset.epochSecond.toDouble()
        var hi = lo
        // Bracket the first descending crossing, not the following morning.
        repeat(72) {
            if(altitude(hi,location) > -depression) hi += 300
        }
        require(altitude(hi,location) <= -depression) { "No Maghrib twilight crossing" }
        repeat(40) { val mid = (lo + hi) / 2; if(altitude(mid,location) > -depression) lo = mid else hi = mid }
        return Instant.ofEpochSecond((hi / 60).roundToLong() * 60)
    }
    private fun altitude(epoch: Double, l: LocationSettings): Double {
        val t = (epoch / 86400 + 2440587.5 - 2451545.0) / 36525
        val longitude = ((280.46646 + t * (36000.76983 + t * .0003032)) % 360 + 360) % 360
        val anomaly = 357.52911 + t * (35999.05029 - .0001537 * t)
        val eccentricity = .016708634 - t * (.000042037 + .0000001267 * t)
        val m = Math.toRadians(anomaly)
        val center = sin(m) * (1.914602 - t * (.004817 + .000014 * t)) + sin(2*m) * (.019993 - .000101*t) + sin(3*m)*.000289
        val omega = Math.toRadians(125.04 - 1934.136 * t)
        val lambda = Math.toRadians(longitude + center - .00569 - .00478 * sin(omega))
        val epsilon = Math.toRadians(23 + (26 + (21.448 - t*(46.815 + t*(.00059 - t*.001813))) / 60) / 60 + .00256*cos(omega))
        val declination = asin(sin(epsilon) * sin(lambda))
        val y = tan(epsilon / 2).pow(2)
        val ls = Math.toRadians(longitude)
        val equation = 4 * Math.toDegrees(y*sin(2*ls) - 2*eccentricity*sin(m) + 4*eccentricity*y*sin(m)*cos(2*ls) - .5*y*y*sin(4*ls) - 1.25*eccentricity*eccentricity*sin(2*m))
        val utcMinutes = ((epoch % 86400) + 86400) % 86400 / 60
        val solarMinutes = ((utcMinutes + equation + 4*l.longitude) % 1440 + 1440) % 1440
        val hour = Math.toRadians(solarMinutes / 4 - 180)
        val latitude = Math.toRadians(l.latitude)
        return Math.toDegrees(asin((sin(latitude)*sin(declination) + cos(latitude)*cos(declination)*cos(hour)).coerceIn(-1.0,1.0)))
    }
}
