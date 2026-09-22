package com.studyhub.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val dayMonthYear = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val shortDay = SimpleDateFormat("EEE", Locale.getDefault())

    fun formatDate(millis: Long): String = dayMonthYear.format(Date(millis))
    fun formatTime(millis: Long): String = timeOnly.format(Date(millis))
    fun formatMonthYear(millis: Long): String = monthYear.format(Date(millis))
    fun formatShortDay(millis: Long): String = shortDay.format(Date(millis))
    fun formatRange(start: Long, end: Long): String = "${formatTime(start)} - ${formatTime(end)}"

    /** "Due in 2 days" style copy used on the Home dashboard. */
    fun dueInLabel(dueMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = dueMillis - nowMillis
        if (diff < 0) return "Overdue"
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        return when {
            days >= 2L -> "Due in $days days"
            days == 1L -> "Due tomorrow"
            hours >= 1L -> "Due in $hours hours"
            else -> "Due today"
        }
    }

    fun daysInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, monthZeroBased, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** 0 = Sunday, matching the calendar grid in the design. */
    fun firstWeekdayOfMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, monthZeroBased, 1)
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
                ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}