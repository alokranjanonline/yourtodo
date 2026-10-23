package com.example.sedtodo.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sedtodo.data.model.Task
import com.example.sedtodo.data.model.TaskType
import com.example.sedtodo.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodayDashboardTasks(
    val dailyTasks: List<Task> = emptyList(),
    val fixDateTasks: List<Task> = emptyList(),
    val weeklyTasks: List<Task> = emptyList(),
    val monthlyTasks: List<Task> = emptyList(),
    val yearlyTasks: List<Task> = emptyList()
) {
    val isEmpty: Boolean
        get() = dailyTasks.isEmpty() && fixDateTasks.isEmpty() && weeklyTasks.isEmpty() && monthlyTasks.isEmpty() && yearlyTasks.isEmpty()
}

class TaskViewModel : ViewModel() {
    val allTasks: StateFlow<List<Task>> = TaskRepository.tasks

    val todayDashboardTasks: StateFlow<TodayDashboardTasks> = TaskRepository.tasks.map { tasks ->
        val today = LocalDate.now()
        val scheduledToday = tasks.filter { it.isScheduledForDate(today) }

        TodayDashboardTasks(
            dailyTasks = scheduledToday.filter { it.type == TaskType.DAILY },
            fixDateTasks = scheduledToday.filter { it.type == TaskType.FIX_DATE },
            weeklyTasks = scheduledToday.filter { it.type == TaskType.WEEKLY },
            monthlyTasks = scheduledToday.filter { it.type == TaskType.MONTHLY },
            yearlyTasks = scheduledToday.filter { it.type == TaskType.YEARLY }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayDashboardTasks()
    )

    fun addTask(task: Task) {
        TaskRepository.addTask(task)
    }

    fun updateTask(task: Task) {
        TaskRepository.updateTask(task)
    }

    fun deleteTask(taskId: String) {
        TaskRepository.deleteTask(taskId)
    }

    fun markTaskDone(taskId: String) {
        TaskRepository.markTaskCompleted(taskId, LocalDate.now())
    }

    fun addDailyTask(title: String, description: String) {
        if (title.trim().isNotEmpty()) {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                type = TaskType.DAILY
            )
            TaskRepository.addTask(task)
        }
    }
}
