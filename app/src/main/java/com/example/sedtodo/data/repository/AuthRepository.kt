package com.example.sedtodo.data.repository

import com.example.sedtodo.data.model.User
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun getActionCodeSettings(): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl("https://your-todo-19022.firebaseapp.com/login")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.example.sedtodo",
                true,
                "1"
            )
            .build()
    }

    init {
        // Sync current Firebase Auth user if already signed in and verified
        try {
            val fbUser = auth.currentUser
            if (fbUser != null && fbUser.isEmailVerified) {
                val user = User(
                    id = fbUser.uid,
                    name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                    email = fbUser.email ?: ""
                )
                _currentUser.value = user
            } else {
                _currentUser.value = null
            }
        } catch (_: Exception) {}
    }

    fun register(name: String, email: String, password: String, onResult: (Result<String>) -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()

        if (trimmedEmail.isEmpty() || password.isEmpty() || trimmedName.isEmpty()) {
            onResult(Result.failure(Exception("All fields are required")))
            return
        }

        try {
            auth.createUserWithEmailAndPassword(trimmedEmail, password)
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        // Update profile display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                        fbUser.updateProfile(profileUpdates)

                        // Send Email Verification link with deep link back to app
                        try {
                            fbUser.sendEmailVerification(getActionCodeSettings())
                        } catch (_: Exception) {
                            fbUser.sendEmailVerification()
                        }

                        // Save user in Firestore
                        val userMap = hashMapOf(
                            "id" to fbUser.uid,
                            "name" to trimmedName,
                            "email" to trimmedEmail,
                            "isVerified" to false
                        )
                        firestore.collection("users").document(fbUser.uid).set(userMap)

                        // Sign out so user cannot log in until email is verified
                        auth.signOut()
                        _currentUser.value = null

                        onResult(Result.success("Registration successful! A verification link has been sent to $trimmedEmail. Please check your inbox and click the link to verify before logging in."))
                    } else {
                        onResult(Result.failure(Exception("Registration failed. Please try again.")))
                    }
                }
                .addOnFailureListener { e ->
                    onResult(Result.failure(Exception(e.localizedMessage ?: "Registration failed. Please try again.")))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun login(email: String, password: String, onResult: (Result<User>) -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            onResult(Result.failure(Exception("Email ID and Password are required.")))
            return
        }

        try {
            auth.signInWithEmailAndPassword(trimmedEmail, password)
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        // Reload user state to fetch latest isEmailVerified status from Firebase
                        fbUser.reload().addOnCompleteListener { _ ->
                            if (!fbUser.isEmailVerified) {
                                // User has not verified email yet!
                                auth.signOut()
                                _currentUser.value = null
                                onResult(Result.failure(Exception("Your account has not been verified. Please check your inbox.")))
                            } else {
                                // Verified user: fetch profile and log in
                                firestore.collection("users").document(fbUser.uid).get()
                                    .addOnSuccessListener { doc ->
                                        val name = doc.getString("name") ?: fbUser.displayName ?: trimmedEmail.substringBefore("@")
                                        val user = User(id = fbUser.uid, name = name, email = trimmedEmail)
                                        _currentUser.value = user
                                        onResult(Result.success(user))
                                    }
                                    .addOnFailureListener {
                                        val user = User(id = fbUser.uid, name = fbUser.displayName ?: trimmedEmail.substringBefore("@"), email = trimmedEmail)
                                        _currentUser.value = user
                                        onResult(Result.success(user))
                                    }
                            }
                        }
                    } else {
                        onResult(Result.failure(Exception("Account not found. Please register first.")))
                    }
                }
                .addOnFailureListener { e ->
                    onResult(Result.failure(Exception(e.localizedMessage ?: "Account not found. Please register first.")))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun resendVerificationEmail(email: String, password: String, onResult: (Result<String>) -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            onResult(Result.failure(Exception("Please enter your email and password.")))
            return
        }

        try {
            auth.signInWithEmailAndPassword(trimmedEmail, password)
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        try {
                            fbUser.sendEmailVerification(getActionCodeSettings())
                        } catch (_: Exception) {
                            fbUser.sendEmailVerification()
                        }
                        auth.signOut()
                        _currentUser.value = null
                        onResult(Result.success("Verification link resent to $trimmedEmail. Please check your inbox."))
                    }
                }
                .addOnFailureListener { e ->
                    onResult(Result.failure(Exception(e.localizedMessage ?: "Could not resend verification link.")))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun sendPasswordResetEmail(email: String): Result<String> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            return Result.failure(Exception("Please enter a valid email address."))
        }

        try {
            auth.sendPasswordResetEmail(trimmedEmail)
        } catch (_: Exception) {}

        return Result.success("Password reset link sent to $trimmedEmail. Please check your inbox.")
    }

    fun updateProfile(newEmail: String, newPassword: String): Result<User> {
        val currUser = _currentUser.value ?: return Result.failure(Exception("User not logged in."))
        val trimmedEmail = newEmail.trim().lowercase()

        if (trimmedEmail.isEmpty()) {
            return Result.failure(Exception("Email cannot be empty."))
        }

        val updatedUser = currUser.copy(email = trimmedEmail)

        try {
            val fbUser = auth.currentUser
            if (fbUser != null) {
                if (trimmedEmail != currUser.email) {
                    fbUser.updateEmail(trimmedEmail)
                }
                if (newPassword.isNotEmpty()) {
                    fbUser.updatePassword(newPassword)
                }
                firestore.collection("users").document(fbUser.uid).update("email", trimmedEmail)
            }
        } catch (_: Exception) {}

        _currentUser.value = updatedUser
        return Result.success(updatedUser)
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }
}
