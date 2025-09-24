package com.kairos.ast.ui.reguistro

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.model.DeviceIdManager
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// Estados para comunicar el resultado de la operación de registro a la UI
sealed class RegistroState {
    object Loading : RegistroState()
    object Success : RegistroState()
    data class Error(val message: String) : RegistroState()
}

class RegistroViewModel : ViewModel() {

    private val _registroState = MutableLiveData<RegistroState>()
    val registroState: LiveData<RegistroState> = _registroState

    companion object {
        private const val DIAS_PRUEBA_GRATUITA = 3
    }

    fun realizarRegistro(
        context: Context,
        nombre: String,
        emailInput: String,
        telefono: String,
        passwordInput: String
    ) {
        _registroState.value = RegistroState.Loading
        viewModelScope.launch {
            try {
                // 1. Validar que el dispositivo no haya usado la prueba
                val deviceIdHash = DeviceIdManager.getSecureDeviceId(context)
                if (DeviceIdManager.hasDeviceUsedFreeTrial(deviceIdHash)) {
                    throw Exception("Este dispositivo ya ha utilizado el período de prueba gratuito.")
                }

                // 2. Registrar usuario en Supabase Auth
                SupabaseClient.client.auth.signUpWith(Email) {
                    email = emailInput
                    password = passwordInput
                }
                val user = SupabaseClient.client.auth.currentUserOrNull()
                    ?: throw Exception("No se pudo obtener el usuario recién creado")

                // 3. Crear el objeto Usuario para la base de datos
                val ahora = Instant.now()
                val expiracion = ahora.plus(DIAS_PRUEBA_GRATUITA.toLong(), ChronoUnit.DAYS)
                val nuevoUsuario = Usuario(
                    id = user.id,
                    email = emailInput,
                    nombre = nombre,
                    telefono = if (telefono.isNotEmpty()) telefono else null,
                    tipo_plan = "gratuito",
                    estado_plan = "activo",
                    dias_Plan = DIAS_PRUEBA_GRATUITA,
                    email_verificado = true,
                    fecha_registro = ahora,
                    fecha_expiracion_plan = expiracion
                )

                // 4. Insertar usuario en la tabla 'usuarios'
                SupabaseClient.client.from("usuarios").insert(nuevoUsuario)

                // 5. Registrar el dispositivo
                DeviceIdManager.registrarDispositivo(deviceIdHash, user.id)

                // 6. Notificar éxito
                _registroState.postValue(RegistroState.Success)

            } catch (e: Exception) {
                val errorMessage = mapError(e)
                _registroState.postValue(RegistroState.Error(errorMessage))
            }
        }
    }

    private fun mapError(error: Exception): String {
        return when {
            error.message?.contains("Este dispositivo ya ha utilizado el período de prueba") == true ->
                error.message!!
            error.message?.contains("User already registered", ignoreCase = true) == true ->
                "Este email ya está registrado."
            error.message?.contains("Password should be at least", ignoreCase = true) == true ->
                "La contraseña no cumple los requisitos mínimos."
            error.message?.contains("network", ignoreCase = true) == true ->
                "Error de conexión. Verifica tu internet."
            else -> "Error al crear la cuenta: ${error.message}"
        }
    }
}