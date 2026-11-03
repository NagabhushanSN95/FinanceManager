package com.nagabhushansn95.financetracker.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Dates and timestamps are stored as ISO-8601 text so the database stays readable (and sortable) in any SQLite tool.
 * Dates: `2026-11-03`. Timestamps: `2026-11-03T09:02:55.123`.
 */
class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? =
        value?.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.format(TIMESTAMP_FORMAT)

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, TIMESTAMP_FORMAT) }

    companion object {
        val TIMESTAMP_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    }
}
