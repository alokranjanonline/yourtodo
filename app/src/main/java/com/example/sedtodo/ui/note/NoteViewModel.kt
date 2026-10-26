package com.example.sedtodo.ui.note

import androidx.lifecycle.ViewModel
import com.example.sedtodo.data.model.Note
import com.example.sedtodo.data.repository.NoteRepository
import kotlinx.coroutines.flow.StateFlow

class NoteViewModel : ViewModel() {
    val notes: StateFlow<List<Note>> = NoteRepository.notes

    fun addNote(title: String, content: String) {
        if (title.trim().isNotEmpty() || content.trim().isNotEmpty()) {
            NoteRepository.addNote(title, content)
        }
    }

    fun updateNote(note: Note) {
        if (note.title.trim().isNotEmpty() || note.content.trim().isNotEmpty()) {
            NoteRepository.updateNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        NoteRepository.deleteNote(noteId)
    }
}
