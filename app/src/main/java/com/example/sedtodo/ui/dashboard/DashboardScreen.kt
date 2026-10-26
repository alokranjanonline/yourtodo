package com.example.sedtodo.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sedtodo.data.model.Task
import com.example.sedtodo.data.model.TaskType
import com.example.sedtodo.ui.components.NativeAdCard
import com.example.sedtodo.ui.task.TaskViewModel
import com.example.sedtodo.ui.theme.getColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    userName: String = "User",
    isAdFreeUser: Boolean = false,
    onRemoveAdsClick: () -> Unit = {}
) {
    val dashboardTasks by taskViewModel.todayDashboardTasks.collectAsState()
    var showAddDailyTaskDialog by remember { mutableStateOf(false) }

    // Floating position offset for draggable FAB
    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    var fabOffsetY by remember { mutableFloatStateOf(0f) }

    val todayDateFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Dashboard Header
            Text(
                text = "Hello, $userName! 👋",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Today: $todayDateFormatted",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dashboardTasks.isEmpty) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        NativeAdCard(
                            isAdFreeUser = isAdFreeUser,
                            onRemoveAdsClick = onRemoveAdsClick
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                                    contentDescription = "No tasks",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All caught up for today!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Use the draggable + button or Manage Tasks to add items.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Native Ad
                    item {
                        NativeAdCard(
                            isAdFreeUser = isAdFreeUser,
                            onRemoveAdsClick = onRemoveAdsClick
                        )
                    }

                    // 1. Daily Tasks
                    if (dashboardTasks.dailyTasks.isNotEmpty()) {
                        item {
                            DashboardTaskSectionHeader(
                                title = "Daily Tasks",
                                type = TaskType.DAILY,
                                count = dashboardTasks.dailyTasks.size
                            )
                        }
                        items(dashboardTasks.dailyTasks, key = { it.id }) { task ->
                            DashboardTaskCard(
                                task = task,
                                onMarkDone = { taskViewModel.markTaskDone(task.id) }
                            )
                        }
                    }

                    // 2. Fix Date Tasks
                    if (dashboardTasks.fixDateTasks.isNotEmpty()) {
                        item {
                            DashboardTaskSectionHeader(
                                title = "Fix Date Tasks",
                                type = TaskType.FIX_DATE,
                                count = dashboardTasks.fixDateTasks.size
                            )
                        }
                        items(dashboardTasks.fixDateTasks, key = { it.id }) { task ->
                            DashboardTaskCard(
                                task = task,
                                onMarkDone = { taskViewModel.markTaskDone(task.id) }
                            )
                        }
                    }

                    // 3. Weekly Tasks
                    if (dashboardTasks.weeklyTasks.isNotEmpty()) {
                        item {
                            DashboardTaskSectionHeader(
                                title = "Weekly Tasks",
                                type = TaskType.WEEKLY,
                                count = dashboardTasks.weeklyTasks.size
                            )
                        }
                        items(dashboardTasks.weeklyTasks, key = { it.id }) { task ->
                            DashboardTaskCard(
                                task = task,
                                onMarkDone = { taskViewModel.markTaskDone(task.id) }
                            )
                        }
                    }

                    // 4. Monthly Tasks
                    if (dashboardTasks.monthlyTasks.isNotEmpty()) {
                        item {
                            DashboardTaskSectionHeader(
                                title = "Monthly Tasks",
                                type = TaskType.MONTHLY,
                                count = dashboardTasks.monthlyTasks.size
                            )
                        }
                        items(dashboardTasks.monthlyTasks, key = { it.id }) { task ->
                            DashboardTaskCard(
                                task = task,
                                onMarkDone = { taskViewModel.markTaskDone(task.id) }
                            )
                        }
                    }

                    // 5. Yearly Tasks
                    if (dashboardTasks.yearlyTasks.isNotEmpty()) {
                        item {
                            DashboardTaskSectionHeader(
                                title = "Yearly Tasks",
                                type = TaskType.YEARLY,
                                count = dashboardTasks.yearlyTasks.size
                            )
                        }
                        items(dashboardTasks.yearlyTasks, key = { it.id }) { task ->
                            DashboardTaskCard(
                                task = task,
                                onMarkDone = { taskViewModel.markTaskDone(task.id) }
                            )
                        }
                    }
                }
            }
        }

        // Draggable FAB overlay
        FloatingActionButton(
            onClick = { showAddDailyTaskDialog = true },
            containerColor = TaskType.DAILY.getColors().primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        fabOffsetX += dragAmount.x
                        fabOffsetY += dragAmount.y
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Daily Task (Draggable)"
            )
        }
    }

    // Floating Action Add Daily Task Dialog
    if (showAddDailyTaskDialog) {
        AddDailyTaskDialog(
            onDismiss = { showAddDailyTaskDialog = false },
            onSubmit = { title, description ->
                taskViewModel.addDailyTask(title, description)
                showAddDailyTaskDialog = false
            }
        )
    }
}

@Composable
fun DashboardTaskSectionHeader(
    title: String,
    type: TaskType,
    count: Int
) {
    val colors = type.getColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(colors.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.container)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onContainer
            )
        }
    }
}

@Composable
fun DashboardTaskCard(
    task: Task,
    onMarkDone: () -> Unit
) {
    val colors = task.type.getColors()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onMarkDone() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onMarkDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark Done",
                        tint = colors.primary
                    )
                }
            }

            // Task Type badge positioned at bottom right corner below task description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.type.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AddDailyTaskDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isTitleError by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Daily Task",
                fontWeight = FontWeight.Bold,
                color = TaskType.DAILY.getColors().primary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (isTitleError && it.trim().isNotEmpty()) {
                            isTitleError = false
                        }
                    },
                    label = { Text("Task Title *") },
                    singleLine = true,
                    isError = isTitleError,
                    supportingText = {
                        if (isTitleError) {
                            Text(
                                text = "Task Title is required!",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.trim().isEmpty()) {
                        isTitleError = true
                        focusRequester.requestFocus()
                    } else {
                        onSubmit(title, description)
                    }
                }
            ) {
                Text("Submit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
