package com.example.sedtodo.ui.auth

import androidx.lifecycle.ViewModel
import com.example.sedtodo.data.model.User
import com.example.sedtodo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val nameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,
    val currentUser: User? = null
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Synchronize logged-in user
        _uiState.value = _uiState.value.copy(currentUser = AuthRepository.currentUser.value)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(nameInput = name, errorMessage = null, successMessage = null)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(emailInput = email, errorMessage = null, successMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(passwordInput = password, errorMessage = null, successMessage = null)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        AuthRepository.login(state.emailInput, state.passwordInput) { result ->
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        currentUser = user,
                        errorMessage = null,
                        successMessage = null,
                        passwordInput = "",
                        isLoading = false
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message,
                        successMessage = null,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        AuthRepository.register(state.nameInput, state.emailInput, state.passwordInput) { result ->
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        successMessage = msg,
                        errorMessage = null,
                        passwordInput = "",
                        isLoading = false
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message,
                        successMessage = null,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun resendVerificationLink() {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        AuthRepository.resendVerificationEmail(state.emailInput, state.passwordInput) { result ->
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        successMessage = msg,
                        errorMessage = null,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message,
                        successMessage = null,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun sendPasswordReset(email: String, onComplete: (String) -> Unit) {
        val result = AuthRepository.sendPasswordResetEmail(email)
        result.fold(
            onSuccess = { msg ->
                _uiState.value = _uiState.value.copy(successMessage = msg, errorMessage = null)
                onComplete(msg)
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message, successMessage = null)
            }
        )
    }

    fun updateProfile(newEmail: String, newPassword: String, onSuccess: () -> Unit) {
        val result = AuthRepository.updateProfile(newEmail, newPassword)
        result.fold(
            onSuccess = { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    emailInput = user.email,
                    successMessage = "Profile updated successfully!",
                    errorMessage = null,
                    passwordInput = ""
                )
                onSuccess()
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message, successMessage = null)
            }
        )
    }

    fun logout(onLoggedOut: () -> Unit) {
        AuthRepository.logout()
        _uiState.value = AuthUiState()
        onLoggedOut()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
