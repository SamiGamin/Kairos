package com.kairos.ast.ui.Login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.model.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

sealed class ForgotPasswordState {
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val _forgotPasswordState = MutableLiveData<ForgotPasswordState>()
    val forgotPasswordState: LiveData<ForgotPasswordState> = _forgotPasswordState

    fun sendPasswordReset(email: String) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Por favor, ingresa un correo electrónico válido.")
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Loading
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email)
                _forgotPasswordState.value = ForgotPasswordState.Success("Se ha enviado un correo para restablecer tu contraseña.")
            } catch (e: Exception) {
                _forgotPasswordState.value = ForgotPasswordState.Error(e.message ?: "Ocurrió un error al enviar el correo.")
            }
        }
    }
}