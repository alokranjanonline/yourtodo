package com.example.sedtodo.data.repository

import android.util.Log
import com.example.sedtodo.data.model.Task
import com.example.sedtodo.data.model.TaskType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

object TaskRepository {
    private const val TAG = "FirebaseTask"
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var snapshotListener: ListenerRegistration? = null

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    init {
        // Observe logged-in user state and switch user task collections dynamically
        CoroutineScope(Dispatchers.Main).launch {
            AuthRepository.currentUser.collect { user ->
                if (user != null && user.id.isNotEmpty()) {
                    loadTasksForUser(user.id)
                } else {
                    snapshotListener?.remove()
                    snapshotListener = null
                    _tasks.value = emptyList()
                }
            }
        }
    }

    private fun getCurrentUserId(): String {
        return AuthRepository.currentUser.value?.id ?: ""
    }

    private fun loadTasksForUser(userId: String) {
        snapshotListener?.remove()
        try {
            snapshotListener = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error for user $userId", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        if (snapshot.isEmpty) {
                            // First time user: create default initial sample tasks for this user
                            createInitialTasksForUser(userId)
                        } else {
                            val userTasks = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Task(
                                        id = doc.id,
                                        userId = userId,
                                        title = doc.getString("title") ?: "",
                                        description = doc.getString("description") ?: "",
                                        type = TaskType.valueOf(doc.getString("type") ?: "DAILY"),
                                        daysOfWeek = (doc.get("daysOfWeek") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList(),
                                        dayOfMonth = (doc.getLong("dayOfMonth") ?: 1L).toInt(),
                                        month = (doc.getLong("month") ?: 1L).toInt(),
                                        year = (doc.getLong("year") ?: 2026L).toInt(),
                                        lastCompletedDate = doc.getString("lastCompletedDate"),
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing task doc ${doc.id}", e)
                                    null
                                }
                            }
                            _tasks.value = userTasks
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading tasks for user $userId", e)
        }
    }

    private fun createInitialTasksForUser(userId: String) {
        val today = LocalDate.now()
        val initialTasks = listOf(
            Task(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Morning Exercise & Stretch",
                description = "30 minutes light morning cardio and stretching",
                type = TaskType.DAILY
            ),
            Task(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Submit Weekly Status Report",
                description = "Summarize project milestones and send to manager",
                type = TaskType.WEEKLY,
                daysOfWeek = listOf(today.dayOfWeek.value)
            ),
            Task(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Pay Monthly Bills",
                description = "Electricity, internet, and subscription services",
                type = TaskType.MONTHLY,
                dayOfMonth = today.dayOfMonth
            ),
            Task(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Annual Health Checkup",
                description = "Routine yearly physical exam",
                type = TaskType.YEARLY,
                month = today.monthValue,
                dayOfMonth = today.dayOfMonth
            ),
            Task(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Team Project Deadline",
                description = "Final deliverable presentation",
                type = TaskType.FIX_DATE,
                year = today.year,
                month = today.monthValue,
                dayOfMonth = today.dayOfMonth
            )
        )

        _tasks.value = initialTasks
        initialTasks.forEach { saveTaskToFirestore(it) }
    }

    private fun saveTaskToFirestore(task: Task) {
        val userId = task.userId.ifEmpty { getCurrentUserId() }
        if (userId.isEmpty()) return

        try {
            val taskMap = hashMapOf(
                "id" to task.id,
                "userId" to userId,
                "title" to task.title,
                "description" to task.description,
                "type" to task.type.name,
                "daysOfWeek" to task.daysOfWeek,
                "dayOfMonth" to task.dayOfMonth,
                "month" to task.month,
                "year" to task.year,
                "lastCompletedDate" to task.lastCompletedDate,
                "createdAt" to task.createdAt
            )
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(task.id)
                .set(taskMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Task saved successfully to Firestore for user $userId: ${task.title}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving task to Firestore for user $userId: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "saveTaskToFirestore exception", e)
        }
    }

    fun addTask(task: Task) {
        val userId = getCurrentUserId()
        val newId = if (task.id.isEmpty()) UUID.randomUUID().toString() else task.id
        val newTask = task.copy(id = newId, userId = userId)
        _tasks.value = _tasks.value.filter { it.id != newId } + newTask
        saveTaskToFirestore(newTask)
    }

    fun updateTask(updatedTask: Task) {
        val userId = updatedTask.userId.ifEmpty { getCurrentUserId() }
        val taskWithUser = updatedTask.copy(userId = userId)
        _tasks.value = _tasks.value.map { if (it.id == taskWithUser.id) taskWithUser else it }
        saveTaskToFirestore(taskWithUser)
    }

    fun deleteTask(taskId: String) {
        val userId = getCurrentUserId()
        _tasks.value = _tasks.value.filter { it.id != taskId }
        if (userId.isEmpty()) return

        try {
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Task deleted from Firestore for user $userId: $taskId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting task from Firestore for user $userId: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "deleteTask exception", e)
        }
    }

    fun markTaskCompleted(taskId: String, date: LocalDate = LocalDate.now()) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        when (task.type) {
            TaskType.DAILY, TaskType.FIX_DATE -> {
                deleteTask(taskId)
            }
            TaskType.WEEKLY, TaskType.MONTHLY, TaskType.YEARLY -> {
                val updatedTask = task.copy(lastCompletedDate = date.toString())
                updateTask(updatedTask)
            }
        }
    }
}
