package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object FreshnessPolicy {
    const val DEFAULT_STALE_DAYS = 30L
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun daysSince(epochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Long {
        if (epochMs <= 0L) return Long.MAX_VALUE
        return max(0L, (nowEpochMs - epochMs) / DAY_MS)
    }

    fun isStale(
        epochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        staleThresholdDays: Long = DEFAULT_STALE_DAYS
    ): Boolean {
        return daysSince(epochMs, nowEpochMs) > staleThresholdDays
    }

    fun formatDate(epochMs: Long): String {
        if (epochMs <= 0L) return "Unrecorded"
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(epochMs))
    }

    fun formatAgeLabel(epochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): String {
        val days = daysSince(epochMs, nowEpochMs)
        return when {
            days == 0L -> "Verified today"
            days == 1L -> "1d ago"
            days > DEFAULT_STALE_DAYS -> "${days}d ago · STALE"
            else -> "${days}d ago"
        }
    }
}
