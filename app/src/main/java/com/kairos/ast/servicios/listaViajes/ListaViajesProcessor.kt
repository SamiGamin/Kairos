package com.kairos.ast.servicios.listaViajes

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.api.DirectionsService
import com.kairos.ast.servicios.configuracion.ConfiguracionServicio
import com.kairos.ast.servicios.listaViajes.model.InfoContenedorViaje
import com.kairos.ast.servicios.listaViajes.utils.CalificacionParser
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
     * Busca en la lista de viajes un servicio que cumpla con los criterios de configuración.
     *
     * @param nodoRaiz El nodo raíz de la pantalla actual.
     * @param config La configuración del servicio con todos los criterios de filtrado.
     * @return El AccessibilityNodeInfo del ViewGroup clicable del viaje válido, o null si no se encuentra.
     *         Es responsabilidad del llamador reciclar el nodo devuelto.
     */
    suspend fun encontrarViajeParaAceptar(
        nodoRaiz: AccessibilityNodeInfo,
        config: ConfiguracionServicio
    ): AccessibilityNodeInfo? {
        val contenedoresViaje = encontrarContenedoresDeViaje(nodoRaiz)
        Log.d(TAG_LOG, "Se encontraron ${contenedoresViaje.size} posibles contenedores de viaje.")

        var viajeCompatible: AccessibilityNodeInfo? = null

        for (contenedor in contenedoresViaje) {
            val infoExtraida = extraerInformacionDelContenedor(contenedor)
            Log.d(TAG_LOG, "Procesando contenedor: Origen='${infoExtraida.direccionOrigen}', Destino='${infoExtraida.direccionDestino}'")

            // Criterio 1: Distancia de recogida (el más barato de procesar)
            if (infoExtraida.distanciaRecogidaKm == null || infoExtraida.distanciaRecogidaKm > config.distanciaMaximaRecogidaKm) {
                Log.d(TAG_LOG, "Viaje descartado por distancia de RECOGIDA: ${infoExtraida.distanciaRecogidaKm} km > ${config.distanciaMaximaRecogidaKm} km.")
                contenedor.recycle()
                continue
            }
            Log.i(TAG_LOG, "Distancia de RECOGIDA VÁLIDA: ${infoExtraida.distanciaRecogidaKm} km (Máx: ${config.distanciaMaximaRecogidaKm} km)")

            // Criterio 2: Filtro por calificación (si está activado)
            if (config.filtrarPorCalificacion) {
                val datosCalificacion = infoExtraida.datosCalificacion
                if (datosCalificacion == null) {
                    Log.w(TAG_LOG, "Viaje descartado: No se pudo extraer la calificación y el filtro está activo.")
                    contenedor.recycle()
                    continue
                }
                if (datosCalificacion.calificacion < config.minCalificacion) {
                    Log.d(TAG_LOG, "Viaje descartado por CALIFICACIÓN: ${datosCalificacion.calificacion} < ${config.minCalificacion}")
                    contenedor.recycle()
                    continue
                }
                Log.i(TAG_LOG, "Criterio de calificación SUPERADO.")
            }

            // Criterio 3: Filtro por número de viajes (si está activado)
            if (config.filtrarPorNumeroDeViajes) {
                val datosCalificacion = infoExtraida.datosCalificacion
                if (datosCalificacion == null) {
                    Log.w(TAG_LOG, "Viaje descartado: No se pudo extraer el número de viajes y el filtro está activo.")
                    contenedor.recycle()
                    continue
                }
                if (datosCalificacion.numeroDeViajes < config.minViajes) {
                    Log.d(TAG_LOG, "Viaje descartado por NÚMERO DE VIAJES: ${datosCalificacion.numeroDeViajes} < ${config.minViajes}")
                    contenedor.recycle()
                    continue
                }
                Log.i(TAG_LOG, "Criterio de número de viajes SUPERADO.")
            }

            // Criterio 4: Distancia del viaje A-B (el más costoso, usa API)
            if (esDistanciaViajeValida(infoExtraida, config.distanciaMaximaViajeABKm)) {
                Log.i(TAG_LOG, "¡VIAJE COMPATIBLE ENCONTRADO!")
                viajeCompatible = contenedor // Guardamos el nodo compatible
                break // Salimos del bucle al encontrar el primero
            }

            // Si no es válido por la distancia del viaje, lo reciclamos
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
     * Valida si la distancia del viaje completo (A-B) es aceptable.
     *
     * @return `true` si la distancia es válida, `false` en caso contrario.
     */
    private suspend fun esDistanciaViajeValida(
        info: InfoContenedorViaje,
        maxDistViaje: Float
    ): Boolean {
        if (info.direccionOrigen == null || info.direccionDestino == null) {
            Log.w(TAG_LOG, "Viaje descartado. Faltan direcciones de origen y/o destino para calcular la distancia del viaje.")
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

        return true // Si pasa el filtro de distancia del viaje, es válido
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

        // Usar el parser correcto para extraer calificación y viajes de una vez.
        val datosCalificacion = CalificacionParser.extraer(contenedorViaje)

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

            // Explorar hijos de ViewGroups anidados para encontrar todos los textos
            if (hijo.childCount > 0 && hijo.className.toString().contains("ViewGroup", ignoreCase = true)) {
                for (i in 0 until hijo.childCount) {
                    hijo.getChild(i)?.let { colaHijos.addLast(it) }
                }
            }
            hijo.recycle()
        }

        val direccionOrigen = posiblesDirecciones.getOrNull(0)
        val direccionDestino = posiblesDirecciones.getOrNull(1)

        return InfoContenedorViaje(distanciaRecogida, direccionOrigen, direccionDestino, tiempoViajeAB, distanciaViajeAB, datosCalificacion)
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

        // Prioridad 3: Descartar basura conocida (calificación y viajes ya se procesaron)
        if (texto.startsWith("COL$") || texto.matches(Regex("^[d.,]+$")) || texto.matches(Regex("^\\(\\d+\\)$"))) {
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
