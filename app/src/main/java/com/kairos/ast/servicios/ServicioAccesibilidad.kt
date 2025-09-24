package com.kairos.ast.servicios

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.configuracion.ConfiguracionManager
import com.kairos.ast.servicios.detalles.DialogoTarifaProcessor
import com.kairos.ast.servicios.estado.EstadoServicio
import com.kairos.ast.servicios.listaViajes.ListaViajesProcessor
import com.kairos.ast.servicios.procesadores.ProcesadorDetalleViaje
import com.kairos.ast.servicios.procesadores.ProcesadorRevelado
import com.kairos.ast.servicios.utils.esPantallaDeDetalle
import com.kairos.ast.servicios.utils.intentarClic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Import añadido para el Dumper
import com.kairos.ast.servicios.AccessibilityNodeDumper

class ServicioAccesibilidad : AccessibilityService() {

    private var estadoActual: EstadoServicio = EstadoServicio.BuscandoEnLista
    private lateinit var configManager: ConfiguracionManager
    private var tarifaACalcular: String? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        private const val TAG_LOG = "ServicioAccesibilidad"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG_LOG, "Servicio de Accesibilidad Conectado. Estado inicial: $estadoActual")

        // Inicializa y arranca el manager de configuración
        configManager = ConfiguracionManager(this)
        configManager.iniciar()

        val infoServicio = AccessibilityServiceInfo().apply {
            packageNames = arrayOf("sinet.startup.inDriver")
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        this.serviceInfo = infoServicio
        Log.i(TAG_LOG, "Servicio configurado para escuchar eventos de: ${infoServicio.packageNames.joinToString()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        configManager.destruir() // Detiene el listener de configuración
        Log.i(TAG_LOG, "Servicio destruido.")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        // Asegurarse de que el configManager esté inicializado
        if (!::configManager.isInitialized) return

        val nodoRaiz = rootInActiveWindow ?: return

        try {
            // --- LÓGICA GLOBAL DE DETECCIÓN DE DETALLE ---
            // Si detectamos la pantalla de detalle y estamos en un estado de búsqueda,
            // forzamos la transición para analizar el viaje, sin importar cómo se llegó a él.
            if (estadoActual is EstadoServicio.BuscandoEnLista && nodoRaiz.esPantallaDeDetalle()) {
                Log.i(TAG_LOG, "Pantalla de detalle detectada desde un estado no relacionado. Forzando análisis.")
                transicionarA(EstadoServicio.EnDetalleRevelando)
                // No es necesario hacer más nada en este evento, el próximo se encargará con el nuevo estado.
            }
            // --- FIN DE LÓGICA GLOBAL ---

            when (estadoActual) {
                is EstadoServicio.BuscandoEnLista -> gestionarEstadoBuscandoEnLista(evento, nodoRaiz)
                is EstadoServicio.EsperandoAparicionDetalle -> gestionarEstadoEsperandoDetalle(evento, nodoRaiz)
                is EstadoServicio.EnDetalleRevelando -> gestionarEstadoRevelandoDetalle(nodoRaiz)
                is EstadoServicio.EnDetalleProcesando -> gestionarEstadoProcesandoDetalle(evento, nodoRaiz)
                is EstadoServicio.EsperandoDialogoTarifa -> gestionarEstadoEsperandoDialogo(evento, nodoRaiz)
                is EstadoServicio.EnDialogoTarifa -> { /* No hacer nada aquí, la coroutine se encarga */ }
            }
        } finally {
            nodoRaiz.recycle()
        }
    }

    private fun transicionarA(nuevoEstado: EstadoServicio) {
        if (estadoActual::class != nuevoEstado::class) {
            Log.i(TAG_LOG, "==> Transición de estado: ${estadoActual::class.simpleName} -> ${nuevoEstado::class.simpleName}")
            estadoActual = nuevoEstado
        }
    }

    private fun gestionarEstadoBuscandoEnLista(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            serviceScope.launch {
                val nodoCopia = AccessibilityNodeInfo.obtain(nodoRaiz)
                try {
                    val nodoParaClic = ListaViajesProcessor.encontrarViajeParaAceptar(
                        nodoCopia,
                        configManager.configuracion.distanciaMaximaRecogidaKm,
                        configManager.configuracion.distanciaMaximaViajeABKm
                    )

                    if (nodoParaClic != null) {
                        if (nodoParaClic.intentarClic()) {
                            transicionarA(EstadoServicio.EsperandoAparicionDetalle)
                        } else {
                            Log.w(TAG_LOG, "Se encontró un viaje, pero el clic falló.")
                        }
                        nodoParaClic.recycle()
                    }
                } finally {
                    nodoCopia.recycle()
                }
            }
        }
    }

    private fun gestionarEstadoEsperandoDetalle(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (nodoRaiz.esPantallaDeDetalle()) {
                transicionarA(EstadoServicio.EnDetalleRevelando)
            }
        }
    }

    private fun gestionarEstadoRevelandoDetalle(nodoRaiz: AccessibilityNodeInfo) {
        ProcesadorRevelado.revelarContenido(nodoRaiz)
        // Siempre transicionamos, incluso si falla, para intentar procesar lo que sea visible.
        transicionarA(EstadoServicio.EnDetalleProcesando)
    }

    private fun gestionarEstadoProcesandoDetalle(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (!nodoRaiz.esPantallaDeDetalle()) {
            Log.i(TAG_LOG, "La pantalla de detalle ya no es visible. Volviendo a buscar.")
            transicionarA(EstadoServicio.BuscandoEnLista)
            return
        }

        // DUMP DE NODOS AÑADIDO PARA DEPURACIÓN
        Log.d(TAG_LOG, "Activando NodeDumper para la pantalla de DETALLE.")
        AccessibilityNodeDumper.dumpNodes(nodoRaiz)

        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val siguienteEstado = ProcesadorDetalleViaje.procesar(nodoRaiz, configManager.configuracion) { tarifa ->
                this.tarifaACalcular = tarifa
            }
            transicionarA(siguienteEstado)
        }
    }

    private fun gestionarEstadoEsperandoDialogo(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.i(TAG_LOG, "Detectado cambio de ventana/contenido. Posible aparición de diálogo de tarifa.")
            
            // DUMP DE NODOS AÑADIDO PARA DEPURACIÓN
            Log.d(TAG_LOG, "Activando NodeDumper para el DIÁLOGO DE OFERTA.")
            AccessibilityNodeDumper.dumpNodes(nodoRaiz)

            transicionarA(EstadoServicio.EnDialogoTarifa)
            serviceScope.launch {
                delay(500L) // Pequeña espera para que el diálogo se asiente
                val dialogoNodoRaiz = rootInActiveWindow
                if (dialogoNodoRaiz != null) {
                    gestionarEstadoEnDialogo(dialogoNodoRaiz)
                    dialogoNodoRaiz.recycle()
                } else {
                    Log.w(TAG_LOG, "No se pudo obtener el nodo raíz para el diálogo de tarifa.")
                    transicionarA(EstadoServicio.BuscandoEnLista)
                }
            }
        }
    }

    private fun gestionarEstadoEnDialogo(nodoRaizDialogo: AccessibilityNodeInfo) {
        if (tarifaACalcular != null) {
            Log.i(TAG_LOG, "Intentando ofertar tarifa calculada: $tarifaACalcular")
            DialogoTarifaProcessor.ofertarNuevaTarifa(nodoRaizDialogo, tarifaACalcular!!)
            tarifaACalcular = null
        } else {
            Log.w(TAG_LOG, "Se entró a gestionar diálogo sin tarifa a calcular.")
        }
        transicionarA(EstadoServicio.BuscandoEnLista)
    }

    override fun onInterrupt() {
        Log.w(TAG_LOG, "Servicio de accesibilidad interrumpido.")
        serviceJob.cancel()
    }
}