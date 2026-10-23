package com.example.sedtodo.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.sedtodo.data.model.TaskType

data class TaskTypeColors(
    val primary: Color,
    val container: Color,
    val onContainer: Color
)

fun TaskType.getColors(): TaskTypeColors {
    return when (this) {
        TaskType.DAILY -> TaskTypeColors(
            primary = Color(0xFF00796B),
            container = Color(0xFFE0F2F1),
            onContainer = Color(0xFF004D40)
        )
        TaskType.FIX_DATE -> TaskTypeColors(
            primary = Color(0xFFD32F2F),
            container = Color(0xFFFFEBEE),
            onContainer = Color(0xFF8C1D1D)
        )
        TaskType.WEEKLY -> TaskTypeColors(
            primary = Color(0xFF5E35B1),
            container = Color(0xFFEDE7F6),
            onContainer = Color(0xFF311B92)
        )
        TaskType.MONTHLY -> TaskTypeColors(
            primary = Color(0xFFE65100),
            container = Color(0xFFFFF3E0),
            onContainer = Color(0xFFBF360C)
        )
        TaskType.YEARLY -> TaskTypeColors(
            primary = Color(0xFF2E7D32),
            container = Color(0xFFE8F5E9),
            onContainer = Color(0xFF1B5E20)
        )
    }
}
