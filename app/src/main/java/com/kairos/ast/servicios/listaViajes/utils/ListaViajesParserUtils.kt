package com.kairos.ast.servicios.listaViajes.utils

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.utils.buscarNodos
import java.util.regex.Pattern

private const val TAG_LOG = "ListaViajesParserUtils"

// Regex para la distancia de recogida. Ej: "1.8 km"
private val REGEX_DISTANCIA_RECOGIDA = Regex("""~?\s*([\d,.]+)\s*(km|metro|metros)""", RegexOption.IGNORE_CASE)

private val REGEX_TIEMPO_VIAJE_AB = Regex("""(\d+)\s*min(uto)?s?""", RegexOption.IGNORE_CASE)
private val REGEX_DISTANCIA_VIAJE_AB = Regex("""([\d,.]+)\s*km""", RegexOption.IGNORE_CASE)

// Regex para calificación (ej: "5.0", "4.85").
private val REGEX_CALIFICACION = Pattern.compile("""(\d[\.,]\d{1,2})""")

// Regex para viajes (ej: "(20)", "(350)").
private val REGEX_VIAJES = Pattern.compile("""\((\d+)\)""")

/**
 * Data class para almacenar la calificación y el número de viajes del conductor.
 */
data class DatosCalificacion(
    val calificacion: Float,
    val numeroDeViajes: Int
)

/**
 * Extrae la distancia de recogida en KM de una cadena de texto.
 * @param texto El texto a analizar.
 * @return La distancia en KM como Float, o null si no se encuentra.
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
 * Extrae el tiempo de viaje en minutos de una cadena de texto.
 * @param texto El texto a analizar.
 * @return El tiempo en minutos como Int, o null si no se encuentra.
 */
fun extraerTiempoViajeABEnMinutos(texto: String): Int? {
    val resultado = REGEX_TIEMPO_VIAJE_AB.find(texto) ?: return null
    val (valorStr) = resultado.destructured
    return valorStr.toIntOrNull()
}

/**
 * Extrae la distancia de viaje en KM de una cadena de texto.
 * @param texto El texto a analizar.
 * @return La distancia en KM como Float, o null si no se encuentra.
 */
fun extraerDistanciaViajeABEnKm(texto: String): Float? {
    val resultado = REGEX_DISTANCIA_VIAJE_AB.find(texto) ?: return null
    val (valorStr) = resultado.destructured
    return valorStr.replace(',', '.').toFloatOrNull()
}

/**
 * Extrae la calificación y el número de viajes de una cadena de texto.
 * @param texto El texto a analizar, que puede contener ambos datos.
 * @return Un objeto [DatosCalificacion] si se encuentran ambos valores, o null.
 */
fun extraerCalificacionYViajes(texto: String): DatosCalificacion? {
    val matcherCalificacion = REGEX_CALIFICACION.matcher(texto)
    val calificacion = if (matcherCalificacion.find()) {
        matcherCalificacion.group(1)?.replace(',', '.')?.toFloatOrNull()
    } else {
        null
    }

    val matcherViajes = REGEX_VIAJES.matcher(texto)
    val viajes = if (matcherViajes.find()) {
        matcherViajes.group(1)?.toIntOrNull()
    } else {
        null
    }

    return if (calificacion != null && viajes != null) {
        DatosCalificacion(calificacion, viajes)
    } else {
        null
    }
}

/**
 * Objeto responsable de encontrar y extraer la información de calificación
 * del conductor desde un nodo de accesibilidad, buscando en sus hijos.
 */
object CalificacionParser {
    // Regex para calificación (ej: "5.0", "4.85"). Busca un número con 1 o 2 decimales.
    private val REGEX_CALIFICACION_PARSER = Pattern.compile("""^(\d[\.,]\d{1,2})$""")
    // Regex para viajes (ej: "(20)", "(350)"). Busca un número entre paréntesis.
    private val REGEX_VIAJES_PARSER = Pattern.compile("""^\((\d+)\)$""")

    /**
     * Busca en los nodos hijos de un nodo raíz los TextViews que contienen
     * la calificación y el número de viajes por separado.
     *
     * @param nodoRaiz El nodo contenedor del viaje.
     * @return Un objeto [DatosCalificacion] si se encuentra la información, o null en caso contrario.
     */
    fun extraer(nodoRaiz: AccessibilityNodeInfo): DatosCalificacion? {
        Log.d(TAG_LOG, "Iniciando extracción de calificación en TextViews separados...")

        val textViews = nodoRaiz.buscarNodos { it.className == "android.widget.TextView" }
        Log.d(TAG_LOG, "Encontrados ${textViews.size} TextViews para analizar.")

        var calificacionEncontrada: Float? = null
        var viajesEncontrados: Int? = null

        for (nodo in textViews) {
            val texto = nodo.text?.toString()?.trim() ?: ""
            if (texto.isBlank()) continue

            if (calificacionEncontrada == null) {
                val matcherCalificacion = REGEX_CALIFICACION_PARSER.matcher(texto)
                if (matcherCalificacion.find()) {
                    calificacionEncontrada = matcherCalificacion.group(1)?.replace(',', '.')?.toFloatOrNull()
                }
            }

            if (viajesEncontrados == null) {
                val matcherViajes = REGEX_VIAJES_PARSER.matcher(texto)
                if (matcherViajes.find()) {
                    viajesEncontrados = matcherViajes.group(1)?.toIntOrNull()
                }
            }

            if (calificacionEncontrada != null && viajesEncontrados != null) {
                break
            }
        }

        textViews.forEach { it.recycle() }

        return if (calificacionEncontrada != null && viajesEncontrados != null) {
            DatosCalificacion(calificacion = calificacionEncontrada, numeroDeViajes = viajesEncontrados)
        } else {
            null
        }
    }
}
