package app.noor.prayer.core.common

import android.content.Context
import android.text.format.DateFormat
import app.noor.prayer.domain.ClockFormat

/** Latin-digit "HH:mm" or "h:mm a" pattern; pair with Locale.US so digits never render as Eastern Arabic numerals. */
fun clockPattern(format: ClockFormat, context: Context): String = when (format) {
    ClockFormat.H24 -> "HH:mm"
    ClockFormat.H12 -> "h:mm a"
    ClockFormat.SYSTEM -> if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
}
