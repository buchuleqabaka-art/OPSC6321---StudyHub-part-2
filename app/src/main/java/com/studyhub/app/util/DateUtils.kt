package com.studyhub.app.util

import android.content.Context
import com.studyhub.app.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Locale-aware calendar and deadline formatting for the active app language. */
object DateUtils {

    private fun appLocale(context: Context): Locale =
        context.resources.configuration.locales[0] ?: Locale.ENGLISH

    fun formatDate(context: Context, millis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, appLocale(context)).format(Date(millis))

    fun formatTime(context: Context, millis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, appLocale(context)).format(Date(millis))

    fun formatMonthYear(context: Context, millis: Long): String =
        SimpleDateFormat("MMMM yyyy", appLocale(context)).format(Date(millis))

    fun formatShortDay(context: Context, millis: Long): String =
        SimpleDateFormat("EEE", appLocale(context)).format(Date(millis))

    fun formatRange(context: Context, start: Long, end: Long): String =
        context.getString(R.string.time_range_format, formatTime(context, start), formatTime(context, end))

    /** Localized "Due in two days" copy used on the dashboard. */
    fun dueInLabel(context: Context, dueMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = dueMillis - nowMillis
        if (diff < 0) return context.getString(R.string.due_overdue)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        return when {
            days >= 2L -> context.resources.getQuantityString(R.plurals.due_in_days, days.toInt(), days.toInt())
            days == 1L -> context.getString(R.string.due_tomorrow)
            hours >= 1L -> context.resources.getQuantityString(R.plurals.due_in_hours, hours.toInt(), hours.toInt())
            else -> context.getString(R.string.due_today)
        }
    }

    fun daysInMonth(year: Int, monthZeroBased: Int, locale: Locale = Locale.getDefault()): Int {
        val cal = Calendar.getInstance(locale)
        cal.set(year, monthZeroBased, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** 0 = Sunday, matching Android CalendarView's Sunday-first grid. */
    fun firstWeekdayOfMonth(year: Int, monthZeroBased: Int, locale: Locale = Locale.getDefault()): Int {
        val cal = Calendar.getInstance(locale)
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
