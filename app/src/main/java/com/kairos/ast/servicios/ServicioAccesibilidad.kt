package com.kairos.ast.servicios

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.MainActivity
import com.kairos.ast.servicios.detalles.DetalleViajeParser
import com.kairos.ast.servicios.detalles.DialogoTarifaProcessor
import com.kairos.ast.servicios.listaViajes.ListaViajesProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.accessibilityservice.GestureDescription
import android.graphics.Path


/**
 * Servicio de accesibilidad diseñado para interactuar con la aplicación inDriver,
 * automatizando la aceptación de servicios basados en la distancia.
 */
class ServicioAccesibilidad : AccessibilityService() {

    private enum class EstadoDelServicio {
        BUSCANDO_EN_LISTA,
        ESPERANDO_APARICION_DETALLE,
        EN_PANTALLA_DETALLE,
        ESPERANDO_DIALOGO_TARIFA,
        EN_DIALOGO_TARIFA
    }

    private var estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
    private var distanciaMaximaKmConfigurada: Float = MainActivity.VALOR_POR_DEFECTO_DISTANCIA_KM
    private var distanciaMaximaViajeABKmConfigurada: Float = MainActivity.VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM

    private var gananciaPorKmDeseada: Float = MainActivity.VALOR_POR_DEFECTO_TARIFA_KM
    private var tarifaACalcular: String? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        private const val TAG_LOG = "ServicioAccesibilidad"
        private val TEXTOS_CLAVE_PANTALLA_DETALLE = listOf("Ofrece tu tarifa", "Aceptar por COL$")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG_LOG, "Servicio de Accesibilidad Conectado. Estado inicial: $estadoActual")

        cargarConfiguracion()

        val infoServicio = AccessibilityServiceInfo().apply {
            packageNames = arrayOf("sinet.startup.inDriver")
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        this.serviceInfo = infoServicio
        Log.i(
            TAG_LOG,
            "Servicio configurado para escuchar eventos de: ${infoServicio.packageNames.joinToString()}"
        )
    }

    private fun cargarConfiguracion() {
        val prefs = getSharedPreferences(MainActivity.PREFERENCIAS_APP_KAIROS, Context.MODE_PRIVATE)
        distanciaMaximaKmConfigurada = prefs.getFloat(
            MainActivity.CLAVE_DISTANCIA_MAXIMA_KM_LISTA,
            MainActivity.VALOR_POR_DEFECTO_DISTANCIA_KM
        )
        gananciaPorKmDeseada = prefs.getFloat(
            MainActivity.CLAVE_TARIFA_POR_KM,
            MainActivity.VALOR_POR_DEFECTO_TARIFA_KM
        )
        distanciaMaximaViajeABKmConfigurada = prefs.getFloat(
            MainActivity.CLAVE_DISTANCIA_MAXIMA_VIAJE_AB_KM,
            MainActivity.VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM
        )
        Log.i(
            TAG_LOG,
            "Configuración cargada: Dist. Recogida < $distanciaMaximaKmConfigurada km, Dist. Viaje A-B < $distanciaMaximaViajeABKmConfigurada km, Tarifa: $gananciaPorKmDeseada/km"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        if (evento == null) return

        val nodoRaiz = rootInActiveWindow ?: return

        try {
            when (estadoActual) {
                EstadoDelServicio.BUSCANDO_EN_LISTA -> gestionarEstadoBuscandoEnLista(
                    evento,
                    nodoRaiz
                )
                EstadoDelServicio.ESPERANDO_APARICION_DETALLE -> gestionarEstadoEsperandoDetalle(
                    evento,
                    nodoRaiz
                )
                EstadoDelServicio.EN_PANTALLA_DETALLE -> gestionarEstadoEnDetalle(evento, nodoRaiz)
                EstadoDelServicio.ESPERANDO_DIALOGO_TARIFA -> gestionarEstadoEsperandoDialogo(
                    evento,
                    nodoRaiz
                )
                EstadoDelServicio.EN_DIALOGO_TARIFA -> { /* No hacer nada aquí, espera la coroutine */ }
            }
        } finally {
            nodoRaiz.recycle()
        }
    }


// --- GESTIÓN DE ESTADOS (SIMPLIFICADA) ---

    private fun gestionarEstadoBuscandoEnLista(
        evento: AccessibilityEvent?,
        nodoRaiz: AccessibilityNodeInfo
    ) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Loguear la estructura de nodos de la pantalla de lista de viajes para análisis
            Log.d(TAG_LOG, "Detectado cambio en lista de viajes. Ejecutando NodeDumper...")
            AccessibilityNodeDumper.dumpNodes(nodoRaiz) // Llamada al dumper

            // Lanzar una corrutina para manejar la función suspend
            serviceScope.launch {
                val nodoParaClic = ListaViajesProcessor.encontrarViajeParaAceptar(
                    nodoRaiz,
                    distanciaMaximaKmConfigurada,
                    distanciaMaximaViajeABKmConfigurada
                )

                if (nodoParaClic != null) {
                    if (intentarHacerClic(nodoParaClic)) {
                        Log.i(
                            TAG_LOG,
                            "==> [SERVICIO] Clic realizado con éxito. Transicionando a ESPERANDO_APARICION_DETALLE."
                        )
                        estadoActual = EstadoDelServicio.ESPERANDO_APARICION_DETALLE
                    } else {
                        Log.w(TAG_LOG, "==> [SERVICIO] Se encontró un viaje, pero el clic falló.")
                    }
                    nodoParaClic.recycle()
                }
            }
        }
    }

    private fun gestionarEstadoEsperandoDetalle(
        evento: AccessibilityEvent?,
        nodoRaiz: AccessibilityNodeInfo
    ) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (esLaPantallaDeDetalle(nodoRaiz)) {
                Log.i(
                    TAG_LOG,
                    "==> PANTALLA DE DETALLE VERIFICADA. Transicionando a EN_PANTALLA_DETALLE."
                )
                estadoActual = EstadoDelServicio.EN_PANTALLA_DETALLE
            }
        }
    }

    private fun gestionarEstadoEnDetalle(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (!esLaPantallaDeDetalle(nodoRaiz)) {
            estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            return
        }

        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val infoViaje = DetalleViajeParser.parsear(nodoRaiz)

            Log.i(TAG_LOG, "--- INICIO CÁLCULO DE VIAJE ---")
            Log.i(TAG_LOG, "[INFO] Precio Sugerido: ${infoViaje.precioSugeridoNumerico}")
            Log.i(TAG_LOG, "[API] Distancia REAL: ${infoViaje.distanciaViajeRealKm} km")

            if (infoViaje.distanciaViajeRealKm != null && infoViaje.distanciaViajeRealKm > 0) {
                val precioMinimo = infoViaje.distanciaViajeRealKm * gananciaPorKmDeseada
                Log.i(TAG_LOG, "[CÁLCULO] Precio mínimo aceptable: $precioMinimo")

                if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico >= precioMinimo) {
                    Log.i(TAG_LOG, "[DECISIÓN] Aceptando precio sugerido...")
                    infoViaje.botones.nodoBotonAceptar?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    infoViaje.botones.nodoBotonAceptar?.recycle()
                    estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                } else {
                    val contraoferta = (Math.ceil(precioMinimo / 500.0) * 500).toInt()
                    this.tarifaACalcular = contraoferta.toString()
                    Log.i(TAG_LOG, "[DECISIÓN] Contraofertando: $tarifaACalcular")

                    infoViaje.botones.nodoBotonEditar?.let {
                        hacerClicPorGesto(it)
                        it.recycle()
                        estadoActual = EstadoDelServicio.ESPERANDO_DIALOGO_TARIFA
                    } ?: run {
                        estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                    }
                }
            } else {
                estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            }
        }
    }

    private fun gestionarEstadoEsperandoDialogo(
        evento: AccessibilityEvent?,
        nodoRaiz: AccessibilityNodeInfo
    ) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            estadoActual = EstadoDelServicio.EN_DIALOGO_TARIFA
            serviceScope.launch {
                delay(300L)
                rootInActiveWindow?.let {
                    gestionarEstadoEnDialogo(it)
                    it.recycle()
                }
            }
        }
    }

    private fun gestionarEstadoEnDialogo(nodoRaiz: AccessibilityNodeInfo) {
        if (tarifaACalcular != null) {
            Log.i(TAG_LOG, "[DIÁLOGO] Ofertando tarifa calculada: $tarifaACalcular")
            val exito = DialogoTarifaProcessor.ofertarNuevaTarifa(nodoRaiz, tarifaACalcular!!)
            Log.i(TAG_LOG, "[DIÁLOGO] Resultado de la oferta: ${if(exito) "Éxito" else "Fallo"}")
            tarifaACalcular = null
        }
        Log.i(TAG_LOG, "==> Interacción con diálogo finalizada. Volviendo a buscar viajes.")
        estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
    }


    // --- FUNCIONES AUXILIARES DEL SERVICIO ---

    private fun esLaPantallaDeDetalle(nodoRaiz: AccessibilityNodeInfo): Boolean {
        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            nodo.text?.toString()?.let { texto ->
                if (TEXTOS_CLAVE_PANTALLA_DETALLE.any { clave ->
                        texto.contains(
                            clave,
                            ignoreCase = true
                        )
                    }) return true
            }
            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }
        }
        return false
    }

    private fun intentarHacerClic(nodo: AccessibilityNodeInfo): Boolean {
        var nodoClicable: AccessibilityNodeInfo? = nodo
        var intentos = 0
        while (nodoClicable != null && intentos < 5) {
            Log.d(TAG_LOG, "[DEBUG] Intentando clic en nodo: className=${nodoClicable.className}, text=${nodoClicable.text}, visibleToUser=${nodoClicable.isVisibleToUser}, enabled=${nodoClicable.isEnabled}")
            if (nodoClicable.isClickable && nodoClicable.isVisibleToUser && nodoClicable.isEnabled) {
                val resultado = nodoClicable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG_LOG, "[DEBUG] performAction resultado: $resultado")
                return resultado
            }
            nodoClicable = nodoClicable.parent
            intentos++
        }
        Log.w(TAG_LOG, "No se encontró ningún ancestro clicable o visible para clic.")
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG_LOG, "El servicio de accesibilidad ha sido interrumpido.")
    }
    private fun hacerClicPorGesto(nodo: AccessibilityNodeInfo) {
        val rect = Rect()
        nodo.getBoundsInScreen(rect)
        val path = Path().apply { moveTo(rect.centerX().toFloat(), rect.centerY().toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
