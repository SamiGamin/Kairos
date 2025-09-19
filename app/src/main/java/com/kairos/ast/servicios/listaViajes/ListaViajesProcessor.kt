package com.kairos.ast.servicios.listaViajes

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.api.DirectionsService

/**
 * Un objeto 'singleton' responsable de procesar la pantalla de la lista de viajes.
 * Su principal función es encontrar un viaje que cumpla con los criterios
 * de configuración del usuario, utilizando la API de Directions para la distancia del viaje A-B.
 */
object ListaViajesProcessor {

    private const val TAG_LOG = "ListaViajesProcessor"
    // Expresión regular para la distancia de RECOGIDA (ej: "~ 1.5 km", "800 metros")
    private val REGEX_DISTANCIA_RECOGIDA = Regex("""~?\s*([\d,.]+)\s*(km|metro|metros)""", RegexOption.IGNORE_CASE)
    // Expresión regular para el tiempo del VIAJE A-B (ej: "25 min") - utilizado para logs o futuro uso
    private val REGEX_TIEMPO_VIAJE_AB = Regex("""(\d+)\s*min(uto)?s?""", RegexOption.IGNORE_CASE)
    // Expresión regular para la distancia del VIAJE A-B (ej: "12.5 km") - utilizado para logs o futuro uso
    private val REGEX_DISTANCIA_VIAJE_AB = Regex("""([\d,.]+)\s*km""", RegexOption.IGNORE_CASE)

    /**
     * Busca en la lista de viajes un servicio que cumpla con los criterios de distancia.
     * Utiliza la API de Directions para verificar la distancia del viaje A-B.
     *
     * @param nodoRaiz El nodo raíz de la pantalla actual.
     * @param distanciaMaximaRecogidaKm La distancia máxima en KM para ir a RECOGER al pasajero.
     * @param distanciaMaximaViajeABKm La distancia máxima en KM para el VIAJE COMPLETO (A-B) según la API.
     * @return El AccessibilityNodeInfo del ViewGroup clicable del viaje válido, o null.
     */
    suspend fun encontrarViajeParaAceptar(
        nodoRaiz: AccessibilityNodeInfo,
        distanciaMaximaRecogidaKm: Float,
        distanciaMaximaViajeABKm: Float
    ): AccessibilityNodeInfo? {
        val contenedoresViaje = mutableListOf<AccessibilityNodeInfo>()
        encontrarContenedoresDeViaje(nodoRaiz, contenedoresViaje)

        Log.d(TAG_LOG, "Se encontraron ${contenedoresViaje.size} posibles contenedores de viaje (ViewGroups clicables).")

        for (contenedorViaje in contenedoresViaje) {
            Log.d(TAG_LOG, "Procesando contenedor: ${contenedorViaje.className}")
            val infoExtraida = extraerInformacionDelContenedor(contenedorViaje)

            // Loguear la información extraída, incluyendo tiempo y distancia UI si se detectaron
            Log.d(TAG_LOG, "Información extraída del contenedor: DistRecogida=${infoExtraida.distanciaRecogidaKm}, " +
                    "Origen='${infoExtraida.direccionOrigen}', Destino='${infoExtraida.direccionDestino}', " +
                    "Tiempo UI=${infoExtraida.tiempoViajeABMinutos}, Distancia UI=${infoExtraida.distanciaViajeABKm}")

            if (infoExtraida.distanciaRecogidaKm != null && infoExtraida.distanciaRecogidaKm <= distanciaMaximaRecogidaKm) {
                Log.i(
                    TAG_LOG,
                    "Distancia de RECOGIDA VÁLIDA: ${infoExtraida.distanciaRecogidaKm} km (Máx: $distanciaMaximaRecogidaKm km) para origen '${infoExtraida.direccionOrigen}'"
                )

                if (infoExtraida.direccionOrigen != null && infoExtraida.direccionDestino != null) {
                    Log.d(TAG_LOG, "Obteniendo distancia A-B de API para: O='${infoExtraida.direccionOrigen}', D='${infoExtraida.direccionDestino}'")
                    val infoViajeApi = DirectionsService.obtenerInfoViajePrincipal(
                        infoExtraida.direccionOrigen,
                        infoExtraida.direccionDestino
                    )

                    val distanciaViajeABApiKm = infoViajeApi?.first
                    val tiempoViajeABApiMin = infoViajeApi?.second

                    if (distanciaViajeABApiKm != null) {
                        Log.i(
                            TAG_LOG,
                            "Distancia VIAJE A-B (API): $distanciaViajeABApiKm km (Tiempo API: $tiempoViajeABApiMin min) para O='${infoExtraida.direccionOrigen}'"
                        )
                        if (distanciaViajeABApiKm <= distanciaMaximaViajeABKm) {
                            Log.i(
                                TAG_LOG,
                                "¡VIAJE COMPATIBLE ENCONTRADO! Recogida: ${infoExtraida.distanciaRecogidaKm} km, Viaje A-B (API): $distanciaViajeABApiKm km (Máx: $distanciaMaximaViajeABKm km)."
                            )
                            return contenedorViaje
                        } else {
                            Log.d(TAG_LOG, "Viaje descartado por distancia A-B (API): $distanciaViajeABApiKm km > $distanciaMaximaViajeABKm km.")
                        }
                    } else {
                        Log.w(TAG_LOG, "No se pudo obtener la distancia del viaje A-B desde la API para O='${infoExtraida.direccionOrigen}', D='${infoExtraida.direccionDestino}'.")
                    }
                } else {
                    Log.d(TAG_LOG, "Faltan direcciones de origen y/o destino en el contenedor. Recogida: ${infoExtraida.distanciaRecogidaKm} km.")
                }
            } else if (infoExtraida.distanciaRecogidaKm != null) {
                Log.d(TAG_LOG, "Viaje descartado por distancia de RECOGIDA: ${infoExtraida.distanciaRecogidaKm} km > $distanciaMaximaRecogidaKm km.")
            }
        }
        contenedoresViaje.forEach { it.recycle() }
        return null
    }

    /** Clase interna para almacenar la información extraída de un contenedor de viaje. */
    private data class InfoContenedorViaje(
        val distanciaRecogidaKm: Float?,
        val direccionOrigen: String?,
        val direccionDestino: String?,
        val tiempoViajeABMinutos: Int? = null,
        val distanciaViajeABKm: Float? = null
    )

    private fun extraerInformacionDelContenedor(contenedorViaje: AccessibilityNodeInfo): InfoContenedorViaje {
        var distanciaRecogida: Float? = null
        var tiempoViajeAB: Int? = null
        var distanciaViajeAB: Float? = null
        val posiblesDirecciones = mutableListOf<String>()

        val colaHijos = ArrayDeque<AccessibilityNodeInfo>()
        for (i in 0 until contenedorViaje.childCount) {
            contenedorViaje.getChild(i)?.let { colaHijos.addLast(it) }
        }

        while (colaHijos.isNotEmpty()) {
            val hijo = colaHijos.removeFirst()
            val textoHijo = hijo.text?.toString()

            if (!textoHijo.isNullOrEmpty()) {
                // Prioridad 1: Extraer distancia de recogida y saltar
                if (distanciaRecogida == null && REGEX_DISTANCIA_RECOGIDA.find(textoHijo) != null) {
                    distanciaRecogida = extraerDistanciaRecogidaEnKm(textoHijo)
                    Log.d(TAG_LOG, "Dato extraído (Distancia Recogida): $distanciaRecogida km del texto '$textoHijo'")
                    continue
                }

                // Prioridad 2: Extraer datos de UI (tiempo/distancia) y saltar
                if (REGEX_TIEMPO_VIAJE_AB.find(textoHijo) != null || REGEX_DISTANCIA_VIAJE_AB.find(textoHijo) != null) {
                    if (tiempoViajeAB == null) tiempoViajeAB = extraerTiempoViajeABEnMinutos(textoHijo)
                    if (distanciaViajeAB == null) distanciaViajeAB = extraerDistanciaViajeABEnKm(textoHijo)
                    Log.d(TAG_LOG, "Dato extraído (Tiempo/Distancia UI): '$textoHijo'")
                    continue
                }

                // Prioridad 3: Descartar basura conocida (precio, etc.) y saltar
                if (textoHijo.startsWith("COL$")) {
                    Log.d(TAG_LOG, "Texto descartado (Precio): '$textoHijo'")
                    continue
                }

                // Prioridad 4: Identificar si es una dirección usando una heurística más robusta
                val esPotencialDireccion = textoHijo.length > 10 &&
                        (textoHijo.contains("#") ||
                         textoHijo.contains("(") ||
                         textoHijo.contains("cl", ignoreCase = true) ||
                         textoHijo.contains("kr", ignoreCase = true) ||
                         textoHijo.contains("cr", ignoreCase = true) ||
                         textoHijo.contains("calle", ignoreCase = true) ||
                         textoHijo.contains("carrera", ignoreCase = true) ||
                         textoHijo.contains("tv", ignoreCase = true) ||
                         textoHijo.contains("transversal", ignoreCase = true) ||
                         textoHijo.contains("dg", ignoreCase = true) ||
                         textoHijo.contains("diagonal", ignoreCase = true) ||
                         textoHijo.contains("av", ignoreCase = true) ||
                         textoHijo.contains("avenida", ignoreCase = true) ||
                         textoHijo.contains("portal", ignoreCase = true) ||
                         textoHijo.contains("terminal", ignoreCase = true)
                        )

                val esSoloNumeroEntreParentesis = textoHijo.matches(Regex("^(\\d+)"))

                if (esPotencialDireccion && !esSoloNumeroEntreParentesis) {
                    if (!posiblesDirecciones.contains(textoHijo)) {
                        posiblesDirecciones.add(textoHijo)
                        Log.d(TAG_LOG, "Posible dirección AÑADIDA por heurística: '$textoHijo'")
                    }
                } else {
                    Log.d(TAG_LOG, "Texto descartado como dirección (no pasa heurística): '$textoHijo'")
                }
            }

            // Explorar hijos de ViewGroups anidados
            if (hijo.childCount > 0 && hijo.className.toString().contains("ViewGroup", ignoreCase = true)) {
                for (i in 0 until hijo.childCount) {
                    hijo.getChild(i)?.let { colaHijos.addLast(it) }
                }
            }
        }

        val direccionOrigen = posiblesDirecciones.getOrNull(0)
        val direccionDestino = posiblesDirecciones.getOrNull(1)

        if(direccionOrigen != null) Log.i(TAG_LOG, "Dirección de Origen asignada: '$direccionOrigen'")
        else Log.w(TAG_LOG, "No se pudo determinar la dirección de Origen.")

        if(direccionDestino != null) Log.i(TAG_LOG, "Dirección de Destino asignada: '$direccionDestino'")
        else Log.w(TAG_LOG, "No se pudo determinar la dirección de Destino.")

        return InfoContenedorViaje(distanciaRecogida, direccionOrigen, direccionDestino, tiempoViajeAB, distanciaViajeAB)
    }

    /**
     * Busca recursivamente todos los ViewGroups clicables a partir de un nodo raíz.
     * Estos son candidatos a ser contenedores de ítems de viaje.
     */
    private fun encontrarContenedoresDeViaje(nodoActual: AccessibilityNodeInfo, listaContenedores: MutableList<AccessibilityNodeInfo>) {
        if (nodoActual.className != null && nodoActual.className.toString().contains("ViewGroup", ignoreCase = true) && nodoActual.isClickable) {
            Log.d(TAG_LOG, "ViewGroup clicable encontrado: ${nodoActual.className}, añadiendo a la lista de contenedores.")
            listaContenedores.add(nodoActual)
            return
        }

        for (i in 0 until nodoActual.childCount) {
            val hijo = nodoActual.getChild(i)
            if (hijo != null) {
                encontrarContenedoresDeViaje(hijo, listaContenedores)
            }
        }
    }

    /**
     * Convierte el texto que contiene la distancia de RECOGIDA (ej: "1.5 km", "800 metros")
     * a un valor numérico en kilómetros.
     */
    private fun extraerDistanciaRecogidaEnKm(texto: String): Float? {
        val resultado = REGEX_DISTANCIA_RECOGIDA.find(texto) ?: return null
        val (valorStr, unidad) = resultado.destructured
        val valor = valorStr.replace(',','.').toFloatOrNull() ?: return null
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
    private fun extraerTiempoViajeABEnMinutos(texto: String): Int? {
        val resultado = REGEX_TIEMPO_VIAJE_AB.find(texto) ?: return null
        val (valorStr) = resultado.destructured
        return valorStr.toIntOrNull()
    }

    /**
     * Convierte el texto que contiene la distancia del VIAJE A-B (ej: "12.5 km")
     * a un valor numérico en kilómetros.
     */
    private fun extraerDistanciaViajeABEnKm(texto: String): Float? {
        val resultado = REGEX_DISTANCIA_VIAJE_AB.find(texto) ?: return null
        val (valorStr) = resultado.destructured
        return valorStr.replace(',','.').toFloatOrNull()
    }
}
