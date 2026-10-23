package com.example.sedtodo.data.model

import java.time.LocalDate

enum class TaskType(val displayName: String) {
    DAILY("Daily Task"),
    FIX_DATE("Fix Date Task"),
    WEEKLY("Weekly Task"),
    MONTHLY("Monthly Task"),
    YEARLY("Yearly Task")
}

data class Task(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val type: TaskType = TaskType.DAILY,
    
    // For Weekly Task: Selected days of week (1 = Monday, 7 = Sunday)
    val daysOfWeek: List<Int> = emptyList(),
    
    // For Monthly Task (1-31) & Yearly Task (1-31) & Fix Date Task (1-31)
    val dayOfMonth: Int = 1,
    
    // For Yearly Task (1-12) & Fix Date Task (1-12)
    val month: Int = 1,
    
    // For Fix Date Task
    val year: Int = 2026,
    
    // Completion tracking ("YYYY-MM-DD" formatted string or null)
    val lastCompletedDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isScheduledForDate(date: LocalDate): Boolean {
        val dateStr = date.toString() // "YYYY-MM-DD"
        if (lastCompletedDate == dateStr) {
            // Task has already been completed today
            return false
        }
        
        return when (type) {
            TaskType.DAILY -> true
            TaskType.FIX_DATE -> {
                date.year == year && date.monthValue == month && date.dayOfMonth == dayOfMonth
            }
            TaskType.WEEKLY -> {
                val currentDayOfWeekValue = date.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                daysOfWeek.contains(currentDayOfWeekValue)
            }
            TaskType.MONTHLY -> {
                date.dayOfMonth == dayOfMonth
            }
            TaskType.YEARLY -> {
                date.monthValue == month && date.dayOfMonth == dayOfMonth
            }
        }
    }
}
