package com.example.gala.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared date helpers so screens use the same formatting.
 */
object DateUtils {
    private const val ISO_PATTERN = "yyyy-MM-dd"

    fun currentDateIso(): String {
        val formatter = SimpleDateFormat(ISO_PATTERN, Locale.getDefault())
        return formatter.format(Date())
    }
}
