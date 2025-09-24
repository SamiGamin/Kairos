package com.kairos.ast.servicios.listaViajes

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.api.DirectionsService
import com.kairos.ast.servicios.listaViajes.model.InfoContenedorViaje
import com.kairos.ast.servicios.listaViajes.utils.extraerDistanciaRecogidaEnKm
import com.kairos.ast.servicios.listaViajes.utils.extraerDistanciaViajeABEnKm
import com.kairos.ast.servicios.listaViajes.utils.extraerTiempoViajeABEnMinutos

/**
 * Objeto responsable de procesar la pantalla de la lista de viajes.
 * Su principal función es encontrar un viaje que cumpla con los criterios
 * de configuración del usuario.
 */
object ListaViajesProcessor {

    private val TAG_LOG = ListaViajesProcessor::class.java.simpleName

    /**
     * Busca en la lista de viajes un servicio que cumpla con los criterios de distancia.
     *
     * @param nodoRaiz El nodo raíz de la pantalla actual.
     * @param distanciaMaximaRecogidaKm La distancia máxima en KM para ir a RECOGER al pasajero.
     * @param distanciaMaximaViajeABKm La distancia máxima en KM para el VIAJE COMPLETO (A-B).
     * @return El AccessibilityNodeInfo del ViewGroup clicable del viaje válido, o null si no se encuentra.
     *         Es responsabilidad del llamador reciclar el nodo devuelto.
     */
    suspend fun encontrarViajeParaAceptar(
        nodoRaiz: AccessibilityNodeInfo,
        distanciaMaximaRecogidaKm: Float,
        distanciaMaximaViajeABKm: Float
    ): AccessibilityNodeInfo? {
        val contenedoresViaje = encontrarContenedoresDeViaje(nodoRaiz)
        Log.d(TAG_LOG, "Se encontraron ${contenedoresViaje.size} posibles contenedores de viaje.")

        var viajeCompatible: AccessibilityNodeInfo? = null

        for (contenedor in contenedoresViaje) {
            val infoExtraida = extraerInformacionDelContenedor(contenedor)
            Log.d(TAG_LOG, "Procesando contenedor: Origen='${infoExtraida.direccionOrigen}', Destino='${infoExtraida.direccionDestino}'")

            if (esViajeValido(infoExtraida, distanciaMaximaRecogidaKm, distanciaMaximaViajeABKm)) {
                Log.i(TAG_LOG, "¡VIAJE COMPATIBLE ENCONTRADO!")
                viajeCompatible = contenedor // Guardamos el nodo compatible
                break // Salimos del bucle al encontrar el primero
            }
            // Si no es válido, lo reciclamos para liberar memoria
            contenedor.recycle()
        }

        // Reciclamos los contenedores restantes que no fueron seleccionados
        if (viajeCompatible != null) {
            contenedoresViaje.forEach { if (it != viajeCompatible) it.recycle() }
        } else {
            contenedoresViaje.forEach { it.recycle() }
        }

        return viajeCompatible
    }

    /**
     * Valida si un viaje extraído cumple con los criterios de distancia.
     *
     * @return `true` si el viaje es válido, `false` en caso contrario.
     */
    private suspend fun esViajeValido(
        info: InfoContenedorViaje,
        maxDistRecogida: Float,
        maxDistViaje: Float
    ): Boolean {
        // Criterio 1: Distancia de recogida
        if (info.distanciaRecogidaKm == null || info.distanciaRecogidaKm > maxDistRecogida) {
            Log.d(TAG_LOG, "Viaje descartado por distancia de RECOGIDA: ${info.distanciaRecogidaKm} km > $maxDistRecogida km.")
            return false
        }
        Log.i(TAG_LOG, "Distancia de RECOGIDA VÁLIDA: ${info.distanciaRecogidaKm} km (Máx: $maxDistRecogida km)")

        // Criterio 2: Distancia del viaje A-B (usando API)
        if (info.direccionOrigen == null || info.direccionDestino == null) {
            Log.w(TAG_LOG, "Viaje descartado. Faltan direcciones de origen y/o destino.")
            return false
        }

        val infoViajeApi = DirectionsService.obtenerInfoViajePrincipal(info.direccionOrigen, info.direccionDestino)
        val distanciaApi = infoViajeApi?.first

        if (distanciaApi == null) {
            Log.w(TAG_LOG, "No se pudo obtener la distancia del viaje A-B desde la API. Descartando viaje.")
            return false
        }

        Log.i(TAG_LOG, "Distancia VIAJE A-B (API): $distanciaApi km (Tiempo API: ${infoViajeApi.second} min)")

        if (distanciaApi > maxDistViaje) {
            Log.d(TAG_LOG, "Viaje descartado por distancia A-B (API): $distanciaApi km > $maxDistViaje km.")
            return false
        }

        return true // Si pasa todos los filtros, el viaje es válido
    }

    /**
     * Extrae la información relevante de un nodo contenedor de viaje.
     *
     * @param contenedorViaje El nodo que representa un item de la lista de viajes.
     * @return Un objeto [InfoContenedorViaje] con los datos extraídos.
     */
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
                procesarTextoHijo(textoHijo, posiblesDirecciones) { dist, tiempo, distAB ->
                    if (distanciaRecogida == null) distanciaRecogida = dist
                    if (tiempoViajeAB == null) tiempoViajeAB = tiempo
                    if (distanciaViajeAB == null) distanciaViajeAB = distAB
                }
            }

            // Explorar hijos de ViewGroups anidados
            if (hijo.childCount > 0 && hijo.className.toString().contains("ViewGroup", ignoreCase = true)) {
                for (i in 0 until hijo.childCount) {
                    hijo.getChild(i)?.let { colaHijos.addLast(it) }
                }
            }
            hijo.recycle()
        }

        val direccionOrigen = posiblesDirecciones.getOrNull(0)
        val direccionDestino = posiblesDirecciones.getOrNull(1)

        return InfoContenedorViaje(distanciaRecogida, direccionOrigen, direccionDestino, tiempoViajeAB, distanciaViajeAB)
    }

    /**
     * Procesa una cadena de texto de un nodo hijo para extraer datos.
     */
    private fun procesarTextoHijo(
        texto: String,
        direcciones: MutableList<String>,
        onDataFound: (Float?, Int?, Float?) -> Unit
    ) {
        // Prioridad 1: Extraer distancia de recogida
        val distRecogida = extraerDistanciaRecogidaEnKm(texto)
        if (distRecogida != null) {
            onDataFound(distRecogida, null, null)
            return
        }

        // Prioridad 2: Extraer datos de UI (tiempo/distancia)
        val tiempoAB = extraerTiempoViajeABEnMinutos(texto)
        val distAB = extraerDistanciaViajeABEnKm(texto)
        if (tiempoAB != null || distAB != null) {
            onDataFound(null, tiempoAB, distAB)
            return
        }

        // Prioridad 3: Descartar basura conocida (precio, etc.)
        if (texto.startsWith("COL$")) {
            return
        }

        // Prioridad 4: Identificar si es una dirección
        val esPotencialDireccion = texto.length > 10 && (texto.contains("#") || texto.contains("(") ||
                texto.contains("cl", true) || texto.contains("kr", true) || texto.contains("cr", true) ||
                texto.contains("calle", true) || texto.contains("carrera", true) ||
                texto.contains("tv", true) || texto.contains("transversal", true) ||
                texto.contains("dg", true) || texto.contains("diagonal", true))

        if (esPotencialDireccion && !direcciones.contains(texto)) {
            direcciones.add(texto)
        }
    }

    /**
     * Busca de forma iterativa todos los ViewGroups clicables a partir de un nodo raíz.
     *
     * @param nodoRaiz El nodo desde donde empezar la búsqueda.
     * @return Una lista de nodos `ViewGroup` que son clicables. Es responsabilidad del llamador reciclarlos.
     */
    private fun encontrarContenedoresDeViaje(nodoRaiz: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val listaContenedores = mutableListOf<AccessibilityNodeInfo>()
        val cola = ArrayDeque<AccessibilityNodeInfo>()
        cola.add(nodoRaiz)

        while(cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            if (nodo.className?.toString()?.contains("ViewGroup", true) == true && nodo.isClickable) {
                listaContenedores.add(AccessibilityNodeInfo.obtain(nodo))
            }

            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }

            if (nodo != nodoRaiz) {
                nodo.recycle()
            }
        }
        return listaContenedores
    }
}