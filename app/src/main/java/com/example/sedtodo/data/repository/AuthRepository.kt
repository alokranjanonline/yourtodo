package com.example.sedtodo.data.repository

import com.example.sedtodo.data.model.User
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var userSnapshotListener: ListenerRegistration? = null

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
        refreshUserProfile()
    }

    fun refreshUserProfile() {
        try {
            val fbUser = auth.currentUser
            if (fbUser != null && fbUser.isEmailVerified) {
                userSnapshotListener?.remove()

                val defaultUser = User(
                    id = fbUser.uid,
                    name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                    email = fbUser.email ?: ""
                )
                if (_currentUser.value == null) {
                    _currentUser.value = defaultUser
                }

                // Listen to real-time changes on the user document in Cloud Firestore
                userSnapshotListener = firestore.collection("users").document(fbUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val isAdFree = snapshot.getBoolean("isAdFreeUser") ?: false
                            val name = snapshot.getString("name") ?: defaultUser.name
                            val updatedUser = User(
                                id = fbUser.uid,
                                name = name,
                                email = fbUser.email ?: "",
                                isAdFreeUser = isAdFree
                            )
                            _currentUser.value = updatedUser
                        }
                    }
            } else {
                userSnapshotListener?.remove()
                userSnapshotListener = null
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
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                        fbUser.updateProfile(profileUpdates)

                        try {
                            fbUser.sendEmailVerification(getActionCodeSettings())
                        } catch (_: Exception) {
                            fbUser.sendEmailVerification()
                        }

                        val userMap = hashMapOf(
                            "id" to fbUser.uid,
                            "name" to trimmedName,
                            "email" to trimmedEmail,
                            "isVerified" to false,
                            "isAdFreeUser" to false
                        )
                        firestore.collection("users").document(fbUser.uid).set(userMap, SetOptions.merge())

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
                        fbUser.reload().addOnCompleteListener { _ ->
                            if (!fbUser.isEmailVerified) {
                                auth.signOut()
                                _currentUser.value = null
                                onResult(Result.failure(Exception("Your account has not been verified. Please check your inbox.")))
                            } else {
                                refreshUserProfile()
                                firestore.collection("users").document(fbUser.uid).get()
                                    .addOnSuccessListener { doc ->
                                        val name = doc.getString("name") ?: fbUser.displayName ?: trimmedEmail.substringBefore("@")
                                        val isAdFree = doc.getBoolean("isAdFreeUser") ?: false
                                        val user = User(id = fbUser.uid, name = name, email = trimmedEmail, isAdFreeUser = isAdFree)
                                        
                                        if (!doc.contains("isAdFreeUser")) {
                                            firestore.collection("users").document(fbUser.uid).set(mapOf("isAdFreeUser" to false), SetOptions.merge())
                                        }

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

    fun upgradeToAdFree() {
        val currUser = _currentUser.value ?: return
        val updatedUser = currUser.copy(isAdFreeUser = true)
        _currentUser.value = updatedUser

        try {
            val uid = auth.currentUser?.uid ?: currUser.id
            if (uid.isNotEmpty()) {
                firestore.collection("users").document(uid).set(mapOf("isAdFreeUser" to true), SetOptions.merge())
            }
        } catch (_: Exception) {}
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
                firestore.collection("users").document(fbUser.uid).set(mapOf("email" to trimmedEmail), SetOptions.merge())
            }
        } catch (_: Exception) {}

        _currentUser.value = updatedUser
        return Result.success(updatedUser)
    }

    fun logout() {
        try {
            userSnapshotListener?.remove()
            userSnapshotListener = null
            auth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }
}
