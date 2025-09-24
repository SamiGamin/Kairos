package com.kairos.ast.servicios.configuracion

/**
 * Contiene todos los parámetros de configuración que utiliza el servicio de accesibilidad.
 */
data class ConfiguracionServicio(
    val distanciaMaximaRecogidaKm: Float,
    val distanciaMaximaViajeABKm: Float,
    val gananciaPorKmDeseada: Float,
    val gananciaMinimaViaje: Float
)
