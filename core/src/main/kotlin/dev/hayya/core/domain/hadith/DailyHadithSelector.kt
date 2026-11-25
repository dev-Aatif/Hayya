package dev.hayya.core.domain.hadith

import kotlinx.datetime.LocalDate

/**
 * Deterministically picks a daily Hadith for a given calendar date from the available entries,
 * preferring unread entries while staying stable throughout the same day.
 */
object DailyHadithSelector {

    fun selectForDate(entries: List<HadithEntry>, date: LocalDate): HadithEntry? {
        if (entries.isEmpty()) return null
        val sorted = entries.sortedBy { it.id }
        val pool = sorted.filter { !it.isRead }.ifEmpty { sorted }
        val epochDays = date.toEpochDays().toLong()
        val index = ((epochDays % pool.size) + pool.size).toInt() % pool.size
        return pool[index]
    }
}
