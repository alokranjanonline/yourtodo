package com.example.sedtodo.data.repository

import android.util.Log
import com.example.sedtodo.data.model.Note
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object NoteRepository {
    private const val TAG = "FirebaseNote"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var snapshotListener: ListenerRegistration? = null

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Main).launch {
            AuthRepository.currentUser.collect { user ->
                if (user != null && user.id.isNotEmpty()) {
                    loadNotesForUser(user.id)
                } else {
                    snapshotListener?.remove()
                    snapshotListener = null
                    _notes.value = emptyList()
                }
            }
        }
    }

    private fun getCurrentUserId(): String {
        return AuthRepository.currentUser.value?.id ?: ""
    }

    private fun loadNotesForUser(userId: String) {
        snapshotListener?.remove()
        try {
            snapshotListener = firestore.collection("users")
                .document(userId)
                .collection("notes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Notes snapshot listener error for user $userId", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val userNotes = snapshot.documents.mapNotNull { doc ->
                            try {
                                Note(
                                    id = doc.id,
                                    userId = userId,
                                    title = doc.getString("title") ?: "",
                                    content = doc.getString("content") ?: "",
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing note doc ${doc.id}", e)
                                null
                            }
                        }.sortedByDescending { it.updatedAt }
                        _notes.value = userNotes
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notes for user $userId", e)
        }
    }

    private fun saveNoteToFirestore(note: Note) {
        val userId = note.userId.ifEmpty { getCurrentUserId() }
        if (userId.isEmpty()) return

        try {
            val noteMap = hashMapOf(
                "id" to note.id,
                "userId" to userId,
                "title" to note.title,
                "content" to note.content,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt
            )
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.id)
                .set(noteMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Note saved to Firestore for user $userId: ${note.title}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving note to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "saveNoteToFirestore exception", e)
        }
    }

    fun addNote(title: String, content: String) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        val newNote = Note(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title.trim(),
            content = content.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _notes.value = listOf(newNote) + _notes.value
        saveNoteToFirestore(newNote)
    }

    fun updateNote(note: Note) {
        val userId = note.userId.ifEmpty { getCurrentUserId() }
        val updatedNote = note.copy(userId = userId, updatedAt = System.currentTimeMillis())
        _notes.value = _notes.value.map { if (it.id == updatedNote.id) updatedNote else it }
        saveNoteToFirestore(updatedNote)
    }

    fun deleteNote(noteId: String) {
        val userId = getCurrentUserId()
        _notes.value = _notes.value.filter { it.id != noteId }
        if (userId.isEmpty()) return

        try {
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document(noteId)
                .delete()
        } catch (e: Exception) {
            Log.e(TAG, "deleteNote exception", e)
        }
    }
}
