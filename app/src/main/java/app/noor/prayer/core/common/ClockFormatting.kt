package app.noor.prayer.core.common

import android.content.Context
import android.text.format.DateFormat
import app.noor.prayer.domain.ClockFormat

/** Latin-digit "HH:mm" or "h:mm[ a]" pattern; pair with Locale.US so digits never render as Eastern Arabic numerals.
 *  [includeMeridiem] can be turned off for tight spaces (e.g. the widget's narrow columns), where "4:56 AM" would
 *  otherwise overflow and get ellipsized -- the prayer name already makes AM/PM obvious there. */
fun clockPattern(format: ClockFormat, context: Context, includeMeridiem: Boolean = true): String = when (format) {
    ClockFormat.H24 -> "HH:mm"
    ClockFormat.H12 -> if (includeMeridiem) "h:mm a" else "h:mm"
    ClockFormat.SYSTEM -> if (DateFormat.is24HourFormat(context)) "HH:mm" else if (includeMeridiem) "h:mm a" else "h:mm"
}
