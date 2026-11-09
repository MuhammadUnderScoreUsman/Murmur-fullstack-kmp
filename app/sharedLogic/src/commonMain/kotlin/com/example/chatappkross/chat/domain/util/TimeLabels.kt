package com.example.chatappkross.chat.domain.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object TimeLabels {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun clock(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String? {
        if (epochMillis <= 0L) return null
        return clockOf(local(epochMillis, zone))
    }

    fun listLabel(
        epochMillis: Long,
        nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): String? {
        if (epochMillis <= 0L) return null
        val time = local(epochMillis, zone)
        val now = local(nowMillis, zone)
        return when (time.date) {
            now.date -> clockOf(time)
            now.date.minus(1, DateTimeUnit.DAY) -> "Yesterday"
            else -> {
                val day = "${months[time.month.ordinal - 1]} ${time.day}"
                if (time.year == now.year) day else "$day, ${time.year}"
            }
        }
    }

    fun clockLabel(epochMillis: Long): String? = clock(epochMillis)

    fun listLabelNow(epochMillis: Long): String? = listLabel(epochMillis)

    private fun local(epochMillis: Long, zone: TimeZone): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)

    private fun clockOf(time: LocalDateTime): String =
        "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}