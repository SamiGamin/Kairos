package com.kairos.ast.model

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns


// Resultado sellado para representar todos los posibles resultados de la validación.
sealed class ValidationResult {
    data class Valid(val isAdmin: Boolean) : ValidationResult()
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

    suspend fun validate(context: Context): ValidationResult {
        try {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser == null) {
                Log.d(TAG, "Resultado: No hay usuario logueado.")
                UserRoleManager.saveRole(context, null)
                return ValidationResult.NoUser
            }

            Log.d(TAG, "Usuario autenticado: ID=${currentUser.id}. Obteniendo perfil...")
            val userProfile = SupabaseClient.client.from("usuarios")
                .select(columns = Columns.ALL) { filter { eq("id", currentUser.id) } }
                .decodeAs<List<Usuario>>().firstOrNull() // LÍNEA CORREGIDA

            if (userProfile == null) {
                Log.e(TAG, "Error: Usuario autenticado pero no se encontró su perfil en la base de datos.")
                return ValidationResult.Error("Inconsistencia de datos de usuario.")
            }

            UserRoleManager.saveRole(context, userProfile.rol)
            val isAdmin = userProfile.rol.equals("admin", ignoreCase = true)
            Log.i(TAG, "Rol de usuario guardado: ${userProfile.rol} (isAdmin: $isAdmin)")

            if (!verificarDispositivo(context, currentUser.id)) {
                Log.w(TAG, "Resultado: El dispositivo no es válido para este usuario.")
                return ValidationResult.DeviceNotValid
            }

            val planStatus = PlanManager.verificarEstadoPlan(currentUser.id)
            return when (planStatus) {
                PlanManager.PlanStatus.PAID, PlanManager.PlanStatus.FREE_TRIAL -> {
                    Log.i(TAG, "Resultado: Sesión y plan válidos.")
                    ValidationResult.Valid(isAdmin)
                }
                else -> {
                    Log.w(TAG, "Resultado: El plan no es válido (estado: $planStatus).")
                    ValidationResult.PlanNotValid(planStatus)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la validación de la sesión.", e)
            return ValidationResult.Error(e.message ?: "Error desconocido")
        }
    }

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
            // Aquí no decodificamos, solo verificamos si la respuesta no está vacía
            !result.data.isNullOrBlank() && result.data != "[]"
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar el dispositivo en Supabase.", e)
            true // Fail-open
        }
    }
}