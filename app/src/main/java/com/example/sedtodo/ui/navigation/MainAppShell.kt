package com.example.sedtodo.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sedtodo.ui.auth.AuthViewModel
import com.example.sedtodo.ui.components.AdFreeUpgradeDialog
import com.example.sedtodo.ui.dashboard.DashboardScreen
import com.example.sedtodo.ui.management.TaskManagementScreen
import com.example.sedtodo.ui.note.NoteViewModel
import com.example.sedtodo.ui.note.NotesScreen
import com.example.sedtodo.ui.task.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    authViewModel: AuthViewModel,
    taskViewModel: TaskViewModel,
    noteViewModel: NoteViewModel,
    onLogout: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = Dashboard, 1 = Notes, 2 = Task Management
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAdFreeDialog by remember { mutableStateOf(false) }

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val activity = remember(context) {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    val currentUser = authState.currentUser
    val userName = currentUser?.name ?: "User"
    val isAdFreeUser = currentUser?.isAdFreeUser ?: false

    // Back Button Handler
    BackHandler(enabled = true) {
        if (selectedBottomTab != 0) {
            // 1. If on Notes or Manage Tasks, first navigate back to Dashboard
            selectedBottomTab = 0
        } else {
            // 2. If on Dashboard, check double-press back or show exit confirmation box
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime in 1..2500) {
                showExitConfirmDialog = false
                activity?.finishAffinity()
            } else {
                lastBackPressTime = currentTime
                showExitConfirmDialog = true
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedBottomTab) {
                            0 -> "Your Todo"
                            1 -> "My Notes"
                            else -> "Manage Tasks"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (!isAdFreeUser) {
                        IconButton(onClick = { showAdFreeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Remove Ads ($1.99)",
                                tint = Color(0xFFFF8F00)
                            )
                        }
                    }

                    // Profile & Settings Option at Top Bar
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile & Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.NoteAlt, contentDescription = "My Notes") },
                    label = { Text("Notes") }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Manage Tasks") },
                    label = { Text("Manage Tasks") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedBottomTab) {
                0 -> DashboardScreen(
                    taskViewModel = taskViewModel,
                    userName = userName,
                    isAdFreeUser = isAdFreeUser,
                    onRemoveAdsClick = { showAdFreeDialog = true }
                )
                1 -> NotesScreen(
                    noteViewModel = noteViewModel,
                    isAdFreeUser = isAdFreeUser,
                    onRemoveAdsClick = { showAdFreeDialog = true }
                )
                2 -> TaskManagementScreen(
                    taskViewModel = taskViewModel
                )
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = {
                Text(
                    text = "Exit Your Todo?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to close the app?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        activity?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update Profile Dialog
    if (showProfileDialog) {
        UpdateProfileDialog(
            authViewModel = authViewModel,
            onDismiss = { showProfileDialog = false },
            onRemoveAdsClick = {
                showProfileDialog = false
                showAdFreeDialog = true
            },
            onLogout = {
                showProfileDialog = false
                authViewModel.logout(onLoggedOut = onLogout)
            }
        )
    }

    // Ad Free Upgrade Dialog
    if (showAdFreeDialog) {
        AdFreeUpgradeDialog(
            onDismiss = { showAdFreeDialog = false },
            onConfirmPurchase = {
                authViewModel.upgradeToAdFree()
                showAdFreeDialog = false
            }
        )
    }
}

@Composable
fun UpdateProfileDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val currentUser = uiState.currentUser
    val isAdFreeUser = currentUser?.isAdFreeUser ?: false

    var newEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Update Profile",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Name: ${currentUser?.name ?: "User"}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isAdFreeUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: ⭐ Pro Member (Ad-Free)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8F00)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email ID field
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Email ID") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password (optional)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                if (uiState.successMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.successMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!isAdFreeUser) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRemoveAdsClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Remove Ads ($1.99)",
                            tint = Color(0xFFFF8F00)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Ads ($1.99 One-Time)", fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Logout Button option inside profile dialog
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from Account", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    authViewModel.updateProfile(newEmail, newPassword) {
                        // Success
                    }
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
