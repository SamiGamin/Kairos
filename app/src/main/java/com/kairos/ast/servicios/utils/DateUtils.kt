package com.kairos.ast.servicios.utils

import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

object DateUtils {

    /**
     * Calcula los días restantes entre la fecha actual y una fecha de expiración futura.
     * @param expirationInstant El momento exacto de la expiración (de tipo java.time.Instant).
     * @return El número de días restantes como Long. Devuelve 0 si la fecha ya pasó.
     */
    fun calculateRemainingDays(expirationInstant: Instant?): Long {
        if (expirationInstant == null) {
            return 0L
        }
        return try {
            val now = Instant.now()
            val duration = Duration.between(now, expirationInstant)
            
            // duration.toDays() devuelve los días completos. Usamos max(0, ...) para no mostrar negativos.
            max(0L, duration.toDays())
        } catch (e: Exception) {
            Log.e("DateUtils", "Error al calcular días restantes.", e)
            0L
        }
    }

    /**
     * Formatea un objeto Instant a un formato legible para el usuario.
     * Ejemplo: "24 sep 2025 1:47 AM"
     * @param instant El momento exacto a formatear (de tipo java.time.Instant).
     * @return La fecha formateada como un String, o un texto de error si es nula.
     */
    fun formatReadableDateTime(instant: Instant?): String {
        if (instant == null) {
            return "Fecha no disponible"
        }
        return try {
            // Convertir a la zona horaria del sistema para mostrar la hora local del usuario
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            
            // Definir el formato de salida en español
            val outputFormatter = DateTimeFormatter
                .ofPattern("d MMM yyyy h:mm a", Locale("es", "ES"))

            zonedDateTime.format(outputFormatter)
        } catch (e: Exception) {
            Log.e("DateUtils", "Error al formatear la fecha.", e)
            "Fecha inválida"
        }
    }
}