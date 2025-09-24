package com.kairos.ast.servicios.listaViajes.utils

// Expresiones regulares para parsear los textos de la lista de viajes.
private val REGEX_DISTANCIA_RECOGIDA = Regex("""~?\s*([\d,.]+)\s*(km|metro|metros)""", RegexOption.IGNORE_CASE)
private val REGEX_TIEMPO_VIAJE_AB = Regex("""(\d+)\s*min(uto)?s?""", RegexOption.IGNORE_CASE)
private val REGEX_DISTANCIA_VIAJE_AB = Regex("""([\d,.]+)\s*km""", RegexOption.IGNORE_CASE)

/**
 * Convierte el texto que contiene la distancia de RECOGIDA (ej: "1.5 km", "800 metros")
 * a un valor numérico en kilómetros.
 */
fun extraerDistanciaRecogidaEnKm(texto: String): Float? {
    val resultado = REGEX_DISTANCIA_RECOGIDA.find(texto) ?: return null
    val (valorStr, unidad) = resultado.destructured
    val valor = valorStr.replace(',', '.').toFloatOrNull() ?: return null
    return when {
        unidad.equals("km", ignoreCase = true) -> valor
        unidad.startsWith("metro", ignoreCase = true) -> valor / 1000
        else -> null
    }
}

/**
 * Convierte el texto que contiene el tiempo del VIAJE A-B (ej: "25 min")
 * a un valor numérico en minutos.
 */
fun extraerTiempoViajeABEnMinutos(texto: String): Int? {
    val resultado = REGEX_TIEMPO_VIAJE_AB.find(texto) ?: return null
    val (valorStr) = resultado.destructured
    return valorStr.toIntOrNull()
}

/**
 * Convierte el texto que contiene la distancia del VIAJE A-B (ej: "12.5 km")
 * a un valor numérico en kilómetros.
 */
fun extraerDistanciaViajeABEnKm(texto: String): Float? {
    val resultado = REGEX_DISTANCIA_VIAJE_AB.find(texto) ?: return null
    val (valorStr) = resultado.destructured
    return valorStr.replace(',', '.').toFloatOrNull()
}
