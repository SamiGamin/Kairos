package com.kairos.ast.servicios.procesadores

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.utils.buscarNodos
import java.util.regex.Pattern

private const val TAG_LOG = "ProcesadorCalificacion"

/**
 * Data class para almacenar la calificación y el número de viajes del conductor.
 */
data class DatosCalificacion(
    val calificacion: Float,
    val numeroDeViajes: Int
)

/**
 * Objeto responsable de encontrar y extraer la información de calificación
 * del conductor desde la pantalla.
 */
object ProcesadorCalificacion {

    // Expresión regular para encontrar el patrón "X.X (YYY)"
    // Grupo 1: La calificación (ej. "5.0")
    // Grupo 2: El número de viajes (ej. "35")
    private val REGEX_CALIFICACION = Pattern.compile("""(\d\.\d)\s*\((\d+)\)""")

    /**
     * Busca en la pantalla un TextView que contenga la calificación y el número de viajes,
     * y extrae dicha información.
     *
     * @param nodoRaiz El nodo raíz desde el que empezar la búsqueda.
     * @return Un objeto [DatosCalificacion] si se encuentra la información, o null en caso contrario.
     */
    fun extraer(nodoRaiz: AccessibilityNodeInfo): DatosCalificacion? {
        Log.d(TAG_LOG, "Iniciando extracción de calificación...")

        // 1. Buscar todos los TextViews en la pantalla.
        val textViews = nodoRaiz.buscarNodos { it.className == "android.widget.TextView" }
        Log.d(TAG_LOG, "Encontrados ${textViews.size} TextViews para analizar.")

        for (nodo in textViews) {
            val texto = nodo.text?.toString() ?: ""
            if (texto.isBlank()) continue

            val matcher = REGEX_CALIFICACION.matcher(texto)

            if (matcher.find()) {
                Log.i(TAG_LOG, "Patrón de calificación encontrado en el texto: '$texto'")
                
                try {
                    val calificacionStr = matcher.group(1)
                    val viajesStr = matcher.group(2)

                    if (calificacionStr != null && viajesStr != null) {
                        val calificacion = calificacionStr.toFloat()
                        val numeroDeViajes = viajesStr.toInt()
                        
                        Log.i(TAG_LOG, "Calificación extraída: $calificacion, Número de viajes: $numeroDeViajes")
                        
                        // Limpiar nodos antes de retornar
                        textViews.forEach { it.recycle() }
                        
                        return DatosCalificacion(calificacion, numeroDeViajes)
                    }
                } catch (e: Exception) {
                    Log.e(TAG_LOG, "Error al convertir los datos de calificación desde el texto: '$texto'", e)
                    // Continuar con el siguiente nodo si hay un error de parseo
                }
            }
        }

        Log.w(TAG_LOG, "No se encontró ningún TextView que coincida con el patrón de calificación.")
        // Limpiar nodos si no se encontró nada
        textViews.forEach { it.recycle() }
        return null
    }
}