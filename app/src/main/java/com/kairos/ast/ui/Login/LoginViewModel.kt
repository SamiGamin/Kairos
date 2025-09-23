package com.kairos.ast.ui.Login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.model.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

// Definición de la clase sellada Result para manejar éxito y error
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

data class ValidationErrors(
    val emailError: String? = null,
    val passwordError: String? = null
)

sealed class LoginState {
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    fun validateForm(email: String, password: String): Boolean {
        var emailError: String? = null
        if (email.isEmpty()) {
            emailError = "El email es requerido"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Ingresa un email válido"
        }

        var passwordError: String? = null
        if (password.isEmpty()) {
            passwordError = "La contraseña es requerida"
        } else if (password.length < 6) {
            passwordError = "La contraseña debe tener al menos 6 caracteres"
        }

        val errors = ValidationErrors(emailError, passwordError)
        _validationErrors.value = errors

        return errors.emailError == null && errors.passwordError == null
    }

    fun login(email: String, password: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val result = authenticateWithSupabase(email, password)

                when (result) {
                    is Result.Success -> {
                        _loginState.postValue(LoginState.Success)
                    }
                    is Result.Error -> {
                        _loginState.postValue(LoginState.Error(result.message))
                    }
                }
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error("Error de conexión: ${e.message}"))
            }
        }
    }

    private suspend fun authenticateWithSupabase(email: String, password: String): Result<Unit> {
        return try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Invalid login credentials", ignoreCase = true) == true -> "Correo o contraseña incorrectos."
                e.message?.contains("network", ignoreCase = true) == true -> "Error de conexión. Verifica tu internet."
                else -> e.message ?: "Error de autenticación desconocido."
            }
            Result.Error(errorMessage)
        }
    }
}