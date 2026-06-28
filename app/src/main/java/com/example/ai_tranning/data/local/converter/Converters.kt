package com.example.ai_tranning.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room [TypeConverter]s that let the database persist [Date] values as epoch-millisecond longs.
 *
 * Registered on the database so any entity field typed as [Date] is stored as a nullable `Long`.
 */
class Converters {
    /**
     * Converts a stored epoch-millisecond value back into a [Date].
     *
     * @param value timestamp in epoch milliseconds, or `null`.
     * @return the corresponding [Date], or `null` if [value] is `null`.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    /**
     * Converts a [Date] into its epoch-millisecond representation for storage.
     *
     * @param date the date to convert, or `null`.
     * @return the time in epoch milliseconds, or `null` if [date] is `null`.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}