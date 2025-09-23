package com.kairos.ast.model

import android.util.Log

/**
 * Objeto de ejemplo para gestionar el estado del plan del usuario.
 * REEMPLAZAR CON LÓGICA REAL.
 */
object PlanManager {

    private const val TAG = "PlanManager"

    enum class PlanStatus {
        FREE_TRIAL, // Período de prueba activo
        PAID,       // Plan de pago activo
        EXPIRED,    // Plan expirado
        ERROR       // Error al verificar
    }

    /**
     * Verifica el estado del plan del usuario.
     * Esta es una implementación de ejemplo. Deberías consultar a Supabase aquí.
     */
    suspend fun verificarEstadoPlan(userId: String): PlanStatus {
        Log.w(TAG, "Usando implementación de ejemplo para verificarEstadoPlan. Siempre devuelve PAID.")
        // TODO: Implementar la lógica real para verificar el estado del plan en Supabase.
        // Por ahora, siempre devolvemos que el plan está activo para pruebas.
        return PlanStatus.PAID
    }
}
