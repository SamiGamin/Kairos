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
        EN_PANTALLA_DETALLE, // Estado para hacer scroll
        ESPERANDO_DIALOGO_TARIFA,
        EN_DIALOGO_TARIFA
    }

    private var estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
    private var haHechoScrollEnDetalle = false
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

        // Lanzar todo el manejo de eventos en una corrutina para permitir funciones suspend con delays
        serviceScope.launch {
            val nodoRaiz = rootInActiveWindow ?: return@launch
            try {
                when (estadoActual) {
                    EstadoDelServicio.BUSCANDO_EN_LISTA -> gestionarEstadoBuscandoEnLista(evento, nodoRaiz)
                    EstadoDelServicio.ESPERANDO_APARICION_DETALLE -> gestionarEstadoEsperandoDetalle(evento, nodoRaiz)
                    EstadoDelServicio.EN_PANTALLA_DETALLE -> gestionarEstadoEnDetalle(evento, nodoRaiz)
                    EstadoDelServicio.ESPERANDO_DIALOGO_TARIFA -> gestionarEstadoEsperandoDialogo(evento, nodoRaiz)
                    EstadoDelServicio.EN_DIALOGO_TARIFA -> { /* No hacer nada aquí, espera la coroutine */ }
                }
            } finally {
                nodoRaiz.recycle()
            }
        }
    }


// --- GESTIÓN DE ESTADOS ---
    private suspend fun gestionarEstadoBuscandoEnLista(
        evento: AccessibilityEvent,
        nodoRaiz: AccessibilityNodeInfo
    ) {
        if (evento.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || evento.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
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

    private suspend fun gestionarEstadoEsperandoDetalle(
        evento: AccessibilityEvent,
        nodoRaiz: AccessibilityNodeInfo
    ) {
        if (evento.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || evento.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (esLaPantallaDeDetalle(nodoRaiz)) {
                Log.i(
                    TAG_LOG,
                    "==> PANTALLA DE DETALLE VERIFICADA. Transicionando a EN_PANTALLA_DETALLE."
                )
                estadoActual = EstadoDelServicio.EN_PANTALLA_DETALLE
            }
        }
    }

    private suspend fun gestionarEstadoEnDetalle(evento: AccessibilityEvent, nodoRaiz: AccessibilityNodeInfo) {
        if (!esLaPantallaDeDetalle(nodoRaiz)) {
            estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            return
        }

        if (evento.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // --- LÓGICA DE SCROLL CON DELAY ---
            Log.d(TAG_LOG, "En pantalla de detalle, buscando ImageView de scroll...")
            val scrollImageView = findNode(nodoRaiz) { it.className == "android.widget.ImageView" && it.isClickable && it.text.isNullOrEmpty() && it.contentDescription.isNullOrEmpty() }
            if (scrollImageView != null) {
                Log.i(TAG_LOG, "ImageView de scroll encontrada, haciendo clic para revelar botones.")
                intentarHacerClic(scrollImageView)
                scrollImageView.recycle()
                Log.i(TAG_LOG, "Esperando 500ms para que el scroll termine...")
                delay(500L) // Pausa crucial para la animación de scroll
            } else {
                Log.w(TAG_LOG, "No se encontró la ImageView de scroll, se procederá directamente.")
            }
            // --- FIN LÓGICA DE SCROLL ---

            val nodoRaizActualizado = rootInActiveWindow ?: return // Re-obtener el nodo raíz por si cambió
            val infoViaje = DetalleViajeParser.parsear(nodoRaizActualizado)

            Log.i(TAG_LOG, "--- INICIO CÁLCULO DE VIAJE ---")
            Log.i(TAG_LOG, "[INFO] Precio Sugerido: ${infoViaje.precioSugeridoNumerico}")
            Log.i(TAG_LOG, "[API] Distancia REAL: ${infoViaje.distanciaViajeRealKm} km")

            var tarifaParaOfertar: Int? = null

            if (infoViaje.distanciaViajeRealKm != null && infoViaje.distanciaViajeRealKm > 0) {
                if (infoViaje.distanciaViajeRealKm < 5.0f) {
                    tarifaParaOfertar = 65000
                    Log.i(TAG_LOG, "[CÁLCULO] Viaje < 5km. Usando tarifa fija de $tarifaParaOfertar")
                } else {
                    val precioMinimo = infoViaje.distanciaViajeRealKm * gananciaPorKmDeseada
                    val contraoferta = (Math.ceil(precioMinimo / 500.0) * 500).toInt()
                    tarifaParaOfertar = contraoferta
                    Log.i(TAG_LOG, "[CÁLCULO] Viaje >= 5km. Precio mínimo: $precioMinimo, Oferta redondeada: $tarifaParaOfertar")
                }
            }

            if (tarifaParaOfertar != null) {
                 if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico >= tarifaParaOfertar) {
                    Log.i(TAG_LOG, "[DECISIÓN] Aceptando precio sugerido (${infoViaje.precioSugeridoNumerico}) porque es >= a nuestra tarifa calculada ($tarifaParaOfertar).")
                    infoViaje.botones.nodoBotonAceptar?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    infoViaje.botones.nodoBotonAceptar?.recycle()
                    estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                } else {
                    this.tarifaACalcular = tarifaParaOfertar.toString()
                    Log.i(TAG_LOG, "[DECISIÓN] Contraofertando: $tarifaACalcular")

                    infoViaje.botones.nodoBotonEditar?.let {
                        if (intentarHacerClic(it)) {
                            Log.i(TAG_LOG, "Clic en botón Editar parece exitoso. Esperando diálogo.")
                            estadoActual = EstadoDelServicio.ESPERANDO_DIALOGO_TARIFA
                        } else {
                            Log.w(TAG_LOG, "Clic en botón Editar falló. Volviendo a buscar.")
                            estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                        }
                        it.recycle()
                    } ?: run {
                        Log.w(TAG_LOG, "Se quería contraofertar pero no se encontró el botón de editar.")
                        estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                    }
                }
            } else {
                Log.w(TAG_LOG, "No se pudo calcular una tarifa para ofertar (distancia desconocida). Volviendo a buscar.")
                estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            }
            if (nodoRaizActualizado != nodoRaiz) { nodoRaizActualizado.recycle() }
        }
    }

    private suspend fun gestionarEstadoEsperandoDialogo(
        evento: AccessibilityEvent,
        nodoRaiz: AccessibilityNodeInfo
    ) {
        if (evento.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            estadoActual = EstadoDelServicio.EN_DIALOGO_TARIFA
            delay(300L)
            rootInActiveWindow?.let {
                gestionarEstadoEnDialogo(it)
                it.recycle()
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
    private fun findNode(nodoRaiz: AccessibilityNodeInfo, predicado: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            if (predicado(nodo)) {
                return nodo
            }
            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }
        }
        return null
    }

    private fun esLaPantallaDeDetalle(nodoRaiz: AccessibilityNodeInfo): Boolean {
        return findNode(nodoRaiz) { nodo ->
            TEXTOS_CLAVE_PANTALLA_DETALLE.any { clave ->
                nodo.text?.toString()?.contains(clave, ignoreCase = true) == true
            }
        } != null
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