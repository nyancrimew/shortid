package ch.deletescape.shortid.utils

import java.util.*

fun utcDate(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    seconds: Int = 0,
    milliseconds: Int = 0
): Calendar {
    val cal = GregorianCalendar(TimeZone.getTimeZone("UTC"))
    cal.set(year, month, day, hour, minute, seconds)
    cal.set(Calendar.MILLISECOND, milliseconds)
    return cal
}

fun Calendar.nanoseconds(): Long = timeInMillis * 1000000