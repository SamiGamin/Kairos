package com.kairos.ast.servicios.detalles

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.api.DirectionsService
import kotlinx.coroutines.runBlocking

// Archivo: DetalleViajeParser.kt

/**
 * Un objeto 'singleton' responsable de analizar un AccessibilityNodeInfo
 * de la pantalla de detalle y extraer toda la información en un objeto DetalleViaje.
 */
object DetalleViajeParser {

    private const val TAG_LOG = "DetalleViajeParser"

    // --- CONSTANTES DE PARSEO ---
    private const val ID_ORIGEN_DETALLE = "sinet.startup.inDriver:id/info_textview_pickup"
    private const val ID_DESTINO_DETALLE = "sinet.startup.inDriver:id/info_textview_destination"

    // Expresión regular para el precio del viaje, ej: "COL$ 8,600"
    private val REGEX_PRECIO_VIAJE = Regex("""COL\$\s*([\d.,]+)""")
    // Expresión regular para la duración del viaje, ej: "28 min"
    private val REGEX_DURACION_VIAJE = Regex("""(\d+)\s*min""")
    // Expresión regular para la distancia del viaje, ej: "12.5 km"
    private val REGEX_DISTANCIA_VIAJE = Regex("""([\d,.]+)\s*km""")
    // private val TEXTOS_CLAVE_BOTON_EDITAR = listOf("editar", "modificar", "ofrecer tarifa") // No se usa actualmente

    /**
     * Método principal. Recibe el nodo raíz de la pantalla y devuelve un objeto DetalleViaje.
     */
    fun parsear(nodoRaiz: AccessibilityNodeInfo): DetalleViaje {
        val (origen, destino) = extraerInfoBasica(nodoRaiz)
        val precioSugerido = extraerPrecioSugerido(nodoRaiz)
        val botones = extraerBotones(nodoRaiz)
        val (tiempoEstimadoUI, distanciaEstimadaUI) = extraerEstimacionesDesdeUI(nodoRaiz)

        var distanciaRealApi: Float? = null
        // Solo llamamos a la API de Directions si tenemos origen y destino
        if (origen != null && destino != null) {
            Log.d(TAG_LOG, "Obteniendo distancia real de API para Origen: '$origen', Destino: '$destino'")
            runBlocking { // Considerar mover esto fuera si causa problemas de rendimiento en el hilo principal.
                val infoViajeApi = DirectionsService.obtenerInfoViajePrincipal(origen, destino)
                distanciaRealApi = infoViajeApi?.first
                // Podríamos también usar infoViajeApi?.second (duración de API) si fuera necesario
            }
            Log.d(TAG_LOG, "Distancia real de API obtenida: $distanciaRealApi km")
        } else {
            Log.w(TAG_LOG, "No se pudo obtener origen y/o destino para calcular distancia real de API.")
        }

        return DetalleViaje(
            origen = origen,
            destino = destino,
            precioSugeridoNumerico = precioSugerido,
            distanciaViajeRealKm = distanciaRealApi,
            tiempoEstimadoMinutos = tiempoEstimadoUI,
            distanciaEstimadaKm = distanciaEstimadaUI,
            botones = botones
        )
    }

    /** Extrae el origen y destino usando sus IDs de recurso. */
    private fun extraerInfoBasica(nodoRaiz: AccessibilityNodeInfo): Pair<String?, String?> {
        var origen: String? = null
        var destino: String? = null

        val nodosOrigen = nodoRaiz.findAccessibilityNodeInfosByViewId(ID_ORIGEN_DETALLE)
        if (nodosOrigen.isNotEmpty()) {
            origen = nodosOrigen[0].text?.toString()
            // No es necesario reciclar nodosOrigen[0] si solo se extrae el texto.
            // Reciclar la lista completa después.
        }
        nodosOrigen.forEach { it.recycle() }

        val nodosDestino = nodoRaiz.findAccessibilityNodeInfosByViewId(ID_DESTINO_DETALLE)
        if (nodosDestino.isNotEmpty()) {
            destino = nodosDestino[0].text?.toString()
        }
        nodosDestino.forEach { it.recycle() }

        Log.d(TAG_LOG, "Info Básica: Origen='$origen', Destino='$destino'")
        return Pair(origen, destino)
    }

    /**
     * Busca el texto del precio principal sugerido (no clicable) y lo convierte a Float.
     * Ejemplo: el "COL$8,600" grande que no es un botón.
     */
    private fun extraerPrecioSugerido(nodoRaiz: AccessibilityNodeInfo): Float? {
        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        var precioEncontrado: Float? = null

        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            val textoNodo = nodo.text?.toString()

            if (textoNodo != null && textoNodo.startsWith("COL$") && !nodo.isClickable) {
                // Evitar los botones de "Aceptar por COL$..." o las sugerencias de tarifa que son clicables.
                // El precio que buscamos es usualmente un TextView grande no clicable.
                val matchPrecio = REGEX_PRECIO_VIAJE.find(textoNodo)
                if (matchPrecio != null) {
                    val valorPrecio = matchPrecio.groupValues[1].replace(".", "").replace(",", "").toFloatOrNull()
                    if (valorPrecio != null) {
                        precioEncontrado = valorPrecio
                        Log.d(TAG_LOG, "Precio sugerido (no clicable) encontrado: $valorPrecio del texto '$textoNodo'")
                        break // Encontrado, salir del bucle
                    }
                }
            }
            if (precioEncontrado == null) {
                 for (i in 0 until nodo.childCount) {
                    nodo.getChild(i)?.let { cola.addLast(it) }
                }
            }
            // No reciclar aquí dentro del bucle para no afectar la búsqueda o los nodos de la cola.
            // El nodoRaiz lo maneja el llamador.
        }
        return precioEncontrado
    }

    /**
     * Extrae el tiempo estimado y la distancia estimada del viaje A-B
     * que se muestran juntos en la interfaz de usuario (ej: "28 min 12.5 km").
     */
    private fun extraerEstimacionesDesdeUI(nodoRaiz: AccessibilityNodeInfo): Pair<Int?, Float?> {
        var tiempoEnMinutos: Int? = null
        var distanciaEnKm: Float? = null
        val colaDeNodos = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }

        var nodoContenedorEncontrado = false

        while (colaDeNodos.isNotEmpty()) {
            val nodoActual = colaDeNodos.removeFirst()
            val textoNodo = nodoActual.text?.toString()

            if (!textoNodo.isNullOrEmpty() && textoNodo.contains("min", ignoreCase = true) && textoNodo.contains("km", ignoreCase = true)) {
                Log.d(TAG_LOG, "Posible nodo con estimaciones UI (tiempo y dist): '$textoNodo'")

                REGEX_DURACION_VIAJE.find(textoNodo)?.let {
                    tiempoEnMinutos = it.groupValues[1].toIntOrNull()
                }

                REGEX_DISTANCIA_VIAJE.find(textoNodo)?.let {
                    distanciaEnKm = it.groupValues[1].replace(',', '.').toFloatOrNull()
                }

                if (tiempoEnMinutos != null && distanciaEnKm != null) {
                    Log.i(TAG_LOG, "Estimaciones UI extraídas: Tiempo=$tiempoEnMinutos min, Distancia=$distanciaEnKm km (del texto '$textoNodo')")
                    nodoContenedorEncontrado = true
                    break // Encontramos un nodo que contiene ambos, asumimos que es el correcto.
                }
            }

            if (!nodoContenedorEncontrado) {
                for (i in 0 until nodoActual.childCount) {
                    nodoActual.getChild(i)?.let { colaDeNodos.addLast(it) }
                }
            }
             // No reciclar nodoActual aquí para evitar problemas con la cola o si es el nodoRaiz.
        }
        return Pair(tiempoEnMinutos, distanciaEnKm)
    }


    private fun extraerBotones(nodoRaiz: AccessibilityNodeInfo): BotonesViaje {
        var nodoAceptar: AccessibilityNodeInfo? = null
        var nodoEditar: AccessibilityNodeInfo? = null // El botón sin texto
        val sugerenciasTarifa = mutableListOf<String>()

        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }

        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()

            // Botón Aceptar
            if (nodo.text?.toString()?.contains("Aceptar por", true) == true && nodo.isClickable) {
                nodoAceptar = nodo // Guardamos la referencia, no reciclar aquí.
                Log.d(TAG_LOG, "Botón Aceptar encontrado.")
            }

            // Botón Editar (un `Button` clicable sin texto)
            if (nodo.className == "android.widget.Button" && nodo.isClickable && nodo.text.isNullOrEmpty()) {
                // Esta es una heurística fuerte basada en el NodeDumper.
                // Asumimos que es el único botón clicable sin texto en esta pantalla.
                nodoEditar = nodo
                Log.d(TAG_LOG, "Botón Editar encontrado (Button clicable sin texto)." )
            }
            // Sugerencias de tarifa (ej: "COL$10,500" que son clicables)
            if (nodo.className == "android.widget.Button" && nodo.text?.toString()?.startsWith("COL$") == true && nodo.isClickable) {
                if (nodo != nodoAceptar) { // Asegurarse de que no es el mismo botón de "Aceptar por..."
                    sugerenciasTarifa.add(nodo.text.toString())
                    Log.d(TAG_LOG, "Sugerencia de tarifa encontrada: ${nodo.text}")
                }
            }

            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }
        }
        return BotonesViaje(nodoAceptar, nodoEditar, sugerenciasTarifa.distinct())
    }
}
