package com.example.sedtodo.ui.management

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sedtodo.data.model.Task
import com.example.sedtodo.data.model.TaskType
import com.example.sedtodo.ui.task.TaskViewModel
import com.example.sedtodo.ui.theme.getColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import kotlin.math.absoluteValue

fun getMaxDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        2 -> if (Year.isLeap(year.toLong())) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskManagementScreen(
    taskViewModel: TaskViewModel
) {
    val allTasks by taskViewModel.allTasks.collectAsState()

    val taskTypes = remember {
        listOf(
            TaskType.DAILY,
            TaskType.WEEKLY,
            TaskType.MONTHLY,
            TaskType.YEARLY,
            TaskType.FIX_DATE,
            TaskType.TILL_COMPLETED
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { taskTypes.size })
    val selectedTabIndex = pagerState.currentPage

    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentType = taskTypes[selectedTabIndex]
    val typeColors = currentType.getColors()

    // Form State using TextFieldValue to ensure cursor position control
    var editingTaskId by remember { mutableStateOf<String?>(null) }
    var titleInput by remember { mutableStateOf(TextFieldValue("")) }
    var descriptionInput by remember { mutableStateOf(TextFieldValue("")) }
    var isTitleError by remember { mutableStateOf(false) }
    
    // Weekly: List of selected day values 1 (Mon) .. 7 (Sun)
    var selectedDaysOfWeek by remember { mutableStateOf(listOf(1)) }
    
    // Monthly / Yearly / Fix Date day (1-31)
    var selectedDayOfMonth by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }
    
    // Yearly / Fix Date month (1-12)
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    
    // Fix Date year
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }

    fun resetForm() {
        editingTaskId = null
        titleInput = TextFieldValue("")
        descriptionInput = TextFieldValue("")
        isTitleError = false
        selectedDaysOfWeek = listOf(1)
        selectedDayOfMonth = LocalDate.now().dayOfMonth
        selectedMonth = LocalDate.now().monthValue
        selectedYear = LocalDate.now().year
    }

    // Reset form when sliding to another tab page
    LaunchedEffect(pagerState.currentPage) {
        resetForm()
    }

    fun startEditing(task: Task) {
        editingTaskId = task.id
        titleInput = TextFieldValue(
            text = task.title,
            selection = TextRange(task.title.length)
        )
        descriptionInput = TextFieldValue(
            text = task.description,
            selection = TextRange(task.description.length)
        )
        isTitleError = false
        selectedDaysOfWeek = if (task.daysOfWeek.isNotEmpty()) task.daysOfWeek else listOf(1)
        selectedDayOfMonth = task.dayOfMonth
        selectedMonth = task.month
        selectedYear = task.year

        coroutineScope.launch {
            listState.animateScrollToItem(0)
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Task Configuration",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tab links with smooth slider synchronization
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = typeColors.primary
                )
            }
        ) {
            taskTypes.forEachIndexed { index, type ->
                val isSelected = selectedTabIndex == index
                val colors = type.getColors()
                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                        resetForm()
                    },
                    text = {
                        Text(
                            text = when (type) {
                                TaskType.DAILY -> "Daily Task"
                                TaskType.WEEKLY -> "Weekly Task"
                                TaskType.MONTHLY -> "Monthly Task"
                                TaskType.YEARLY -> "Yearly Task"
                                TaskType.FIX_DATE -> "At Fix Date"
                                TaskType.TILL_COMPLETED -> "Till Completed"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Pager with custom smooth Scale & Alpha page transition effect
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageType = taskTypes[pageIndex]
            val pageColors = pageType.getColors()
            val tasksForPageType = remember(allTasks, pageType) {
                allTasks.filter { it.type == pageType }
            }

            LazyColumn(
                state = if (pageIndex == selectedTabIndex) listState else rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = (
                            (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                        val scale = 0.90f + (1f - 0.90f) * (1f - pageOffset.coerceIn(0f, 1f))
                        val alpha = 0.40f + (1f - 0.40f) * (1f - pageOffset.coerceIn(0f, 1f))

                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ITEM 1: Form Section Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, pageColors.primary, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = pageColors.container.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (editingTaskId == null) "Add ${pageType.displayName}" else "Edit ${pageType.displayName}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = pageColors.primary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Title Text Box
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = {
                                    titleInput = it
                                    if (isTitleError && it.text.trim().isNotEmpty()) {
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

                            // Description Box
                            OutlinedTextField(
                                value = descriptionInput,
                                onValueChange = { descriptionInput = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Type-Specific Inputs
                            when (pageType) {
                                TaskType.DAILY, TaskType.TILL_COMPLETED -> {
                                    // Simple text box & description box only
                                }
                                TaskType.WEEKLY -> {
                                    Text(
                                        text = "Select Week Days:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val weekDaysList = listOf(
                                        1 to "Mon", 2 to "Tue", 3 to "Wed",
                                        4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        weekDaysList.forEach { (dayValue, dayLabel) ->
                                            val isChecked = selectedDaysOfWeek.contains(dayValue)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isChecked) pageColors.container else MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, pageColors.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedDaysOfWeek = if (isChecked) {
                                                            if (selectedDaysOfWeek.size > 1) selectedDaysOfWeek - dayValue else selectedDaysOfWeek
                                                        } else {
                                                            selectedDaysOfWeek + dayValue
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedDaysOfWeek = if (checked == true) {
                                                            selectedDaysOfWeek + dayValue
                                                        } else {
                                                            if (selectedDaysOfWeek.size > 1) selectedDaysOfWeek - dayValue else selectedDaysOfWeek
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = pageColors.primary)
                                                )
                                                Text(
                                                    text = dayLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                TaskType.MONTHLY -> {
                                    Text(
                                        text = "Select Day of Month (1 - 31):",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NumberDropdownSelector(
                                        label = "Day of Month",
                                        selectedValue = selectedDayOfMonth,
                                        range = 1..31,
                                        onSelect = { selectedDayOfMonth = it }
                                    )
                                }
                                TaskType.YEARLY -> {
                                    val maxDays = getMaxDaysInMonth(LocalDate.now().year, selectedMonth)
                                    if (selectedDayOfMonth > maxDays) {
                                        selectedDayOfMonth = maxDays
                                    }

                                    Text(
                                        text = "Select Month & Day:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            MonthDropdownSelector(
                                                selectedMonth = selectedMonth,
                                                onSelect = {
                                                    selectedMonth = it
                                                    val newMaxDays = getMaxDaysInMonth(LocalDate.now().year, it)
                                                    if (selectedDayOfMonth > newMaxDays) {
                                                        selectedDayOfMonth = newMaxDays
                                                    }
                                                }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            NumberDropdownSelector(
                                                label = "Day",
                                                selectedValue = selectedDayOfMonth,
                                                range = 1..maxDays,
                                                onSelect = { selectedDayOfMonth = it }
                                            )
                                        }
                                    }
                                }
                                TaskType.FIX_DATE -> {
                                    // Dynamically calculate valid day range for Fix Date based on Leap Year & Month rules!
                                    val maxDays = getMaxDaysInMonth(selectedYear, selectedMonth)
                                    if (selectedDayOfMonth > maxDays) {
                                        selectedDayOfMonth = maxDays
                                    }

                                    Text(
                                        text = "Select Fix Date (Year, Month, Day):",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            NumberDropdownSelector(
                                                label = "Year",
                                                selectedValue = selectedYear,
                                                range = 2026..2035,
                                                onSelect = {
                                                    selectedYear = it
                                                    val newMaxDays = getMaxDaysInMonth(it, selectedMonth)
                                                    if (selectedDayOfMonth > newMaxDays) {
                                                        selectedDayOfMonth = newMaxDays
                                                    }
                                                }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            MonthDropdownSelector(
                                                selectedMonth = selectedMonth,
                                                onSelect = {
                                                    selectedMonth = it
                                                    val newMaxDays = getMaxDaysInMonth(selectedYear, it)
                                                    if (selectedDayOfMonth > newMaxDays) {
                                                        selectedDayOfMonth = newMaxDays
                                                    }
                                                }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            NumberDropdownSelector(
                                                label = "Day",
                                                selectedValue = selectedDayOfMonth,
                                                range = 1..maxDays,
                                                onSelect = { selectedDayOfMonth = it }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Cancel & Submit buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { resetForm() }) {
                                    Text("Cancel")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        if (titleInput.text.trim().isEmpty()) {
                                            isTitleError = true
                                            focusRequester.requestFocus()
                                        } else {
                                            isTitleError = false
                                            val task = Task(
                                                id = editingTaskId ?: "",
                                                title = titleInput.text.trim(),
                                                description = descriptionInput.text.trim(),
                                                type = pageType,
                                                daysOfWeek = selectedDaysOfWeek,
                                                dayOfMonth = selectedDayOfMonth,
                                                month = selectedMonth,
                                                year = selectedYear
                                            )
                                            if (editingTaskId == null) {
                                                taskViewModel.addTask(task)
                                            } else {
                                                taskViewModel.updateTask(task)
                                            }
                                            resetForm()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = pageColors.primary)
                                ) {
                                    Text(if (editingTaskId == null) "Submit" else "Update", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ITEM 2: Task List Section Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configured ${pageType.displayName}s (${tasksForPageType.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = pageColors.primary
                    )
                }

                // ITEM 3: Configured Task Items
                if (tasksForPageType.isEmpty()) {
                    item {
                        Text(
                            text = "No ${pageType.displayName.lowercase()}s configured yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(tasksForPageType, key = { it.id }) { task ->
                        TaskManagementCard(
                            task = task,
                            onEdit = { startEditing(task) },
                            onDelete = { taskViewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskManagementCard(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = task.type.getColors()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Config Summary
                val configSummary = when (task.type) {
                    TaskType.DAILY -> "Every Day"
                    TaskType.TILL_COMPLETED -> "Displayed daily until completed"
                    TaskType.WEEKLY -> {
                        val daysMap = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                        "Days: " + task.daysOfWeek.mapNotNull { daysMap[it] }.joinToString(", ")
                    }
                    TaskType.MONTHLY -> "Day ${task.dayOfMonth} of every month"
                    TaskType.YEARLY -> {
                        val monthsMap = mapOf(1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr", 5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Aug", 9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec")
                        "${monthsMap[task.month]} ${task.dayOfMonth} every year"
                    }
                    TaskType.FIX_DATE -> {
                        val monthsMap = mapOf(1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr", 5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Aug", 9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec")
                        "${monthsMap[task.month]} ${task.dayOfMonth}, ${task.year}"
                    }
                }
                Text(
                    text = configSummary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberDropdownSelector(
    label: String,
    selectedValue: Int,
    range: IntRange,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { number ->
                DropdownMenuItem(
                    text = { Text(number.toString()) },
                    onClick = {
                        onSelect(number)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDropdownSelector(
    selectedMonth: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val months = listOf(
        1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr",
        5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Aug",
        9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec"
    )
    val currentMonthName = months.find { it.first == selectedMonth }?.second ?: "Jan"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentMonthName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Month") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { (monthValue, monthName) ->
                DropdownMenuItem(
                    text = { Text(monthName) },
                    onClick = {
                        onSelect(monthValue)
                        expanded = false
                    }
                )
            }
        }
    }
}
