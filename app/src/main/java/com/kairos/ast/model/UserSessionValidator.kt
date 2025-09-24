package com.kairos.ast.model

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

// Resultado sellado para representar todos los posibles resultados de la validación.
sealed class ValidationResult {
    object Valid : ValidationResult()
    object NoUser : ValidationResult()
    object DeviceNotValid : ValidationResult()
    data class PlanNotValid(val status: PlanManager.PlanStatus) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Objeto centralizado para validar la sesión de un usuario, su plan y su dispositivo.
 */
object UserSessionValidator {

    private const val TAG = "UserSessionValidator"

    /**
     * Realiza una validación completa del estado del usuario actual.
     * @return Un [ValidationResult] que indica el estado del usuario.
     */
    suspend fun validate(context: Context): ValidationResult {
        try {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser == null) {
                Log.d(TAG, "Resultado: No hay usuario logueado.")
                return ValidationResult.NoUser
            }

            Log.d(TAG, "Usuario encontrado: ID=${currentUser.id}. Verificando dispositivo...")
            if (!verificarDispositivo(context, currentUser.id)) {
                Log.w(TAG, "Resultado: El dispositivo no es válido para este usuario.")
                return ValidationResult.DeviceNotValid
            }

            Log.d(TAG, "Dispositivo válido. Verificando plan...")
            val planStatus = PlanManager.verificarEstadoPlan(currentUser.id)
            return when (planStatus) {
                PlanManager.PlanStatus.PAID, PlanManager.PlanStatus.FREE_TRIAL -> {
                    Log.i(TAG, "Resultado: Sesión y plan válidos.")
                    ValidationResult.Valid
                }
                else -> { // EXPIRED o ERROR
                    Log.w(TAG, "Resultado: El plan no es válido (estado: $planStatus).")
                    ValidationResult.PlanNotValid(planStatus)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la validación de la sesión.", e)
            return ValidationResult.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Comprueba si el dispositivo actual está autorizado para el ID de usuario proporcionado.
     */
    private suspend fun verificarDispositivo(context: Context, userId: String): Boolean {
        return try {
            val deviceIdHash = DeviceIdManager.getSecureDeviceId(context)
            val result = SupabaseClient.client
                .from("dispositivos")
                .select {
                    filter {
                        eq("device_id_hash", deviceIdHash)
                        eq("usuario_id", userId)
                    }
                    limit(1)
                }
            !result.data.isNullOrBlank() && result.data != "[]"
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar el dispositivo en Supabase.", e)
            true // Fail-open: en caso de error de red, permitir el acceso para no bloquear al usuario.
        }
    }
}