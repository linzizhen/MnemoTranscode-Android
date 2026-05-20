package com.mtc.app.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())

    fun formatIsoDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val date = isoFormat.parse(isoString)
            date?.let { displayFormat.format(it) } ?: isoString
        } catch (e: Exception) {
            isoString
        }
    }
}
