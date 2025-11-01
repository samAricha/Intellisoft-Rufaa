package com.teka.rufaa.utils;


import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
fun today(): LocalDateTime {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
}

fun getCurrentTime(): Calendar{
    return Calendar.getInstance()
}


/**
 * Converts a LocalDate to API-compatible date format (d/M/yyyy)
 * Example: LocalDate(2025, 6, 13) -> "13/6/2025"
 */
fun LocalDateTime.toApiDateFormat(): String {
    return "${this.dayOfMonth}/${this.monthNumber}/${this.year}"
}

/**
 * Converts epoch milliseconds to API-compatible date format (d/M/yyyy)
 * Example: 1749805777231 -> "13/6/2025"
 */
fun formatEpochToApiDate(epochMillis: Long): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = epochMillis
    }
    return "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
}


data class KotlinxTime(val hour: Int, val minute: Int) {
    // Convert to kotlinx.datetime.LocalTime
    fun toLocalTime(): LocalTime {
        return LocalTime(hour, minute)
    }

    fun toFormattedString(): String {
        return String.format("%02d:%02d", hour, minute)
    }
}

fun getDayStartAndEnd(timestamp: Long): Pair<Long, Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        // Set to the start of the day
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = calendar.timeInMillis

    // Set to the end of the day
    calendar.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val endOfDay = calendar.timeInMillis

    return Pair(startOfDay, endOfDay)
}

