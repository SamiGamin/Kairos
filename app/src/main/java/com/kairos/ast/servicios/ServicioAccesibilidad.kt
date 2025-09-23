package com.kairos.ast.servicios

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.MainActivity
import com.kairos.ast.servicios.detalles.DetalleViaje
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

class ServicioAccesibilidad : AccessibilityService() {

    private enum class EstadoDelServicio {
        BUSCANDO_EN_LISTA,
        ESPERANDO_APARICION_DETALLE,
        EN_DETALLE_REVELANDO, // Nuevo estado para hacer clic y revelar contenido
        EN_DETALLE_PROCESANDO, // Estado para procesar la información ya visible
        ESPERANDO_DIALOGO_TARIFA,
        EN_DIALOGO_TARIFA
    }

    private var estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
    private var distanciaMaximaKmConfigurada: Float = MainActivity.VALOR_POR_DEFECTO_DISTANCIA_KM
    private var distanciaMaximaViajeABKmConfigurada: Float = MainActivity.VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM
    private var gananciaPorKmDeseada: Float = MainActivity.VALOR_POR_DEFECTO_TARIFA_KM
    private var gananciaMinimaConfigurada: Float = MainActivity.VALOR_POR_DEFECTO_GANANCIA_MINIMA_VIAJE
    private var tarifaACalcular: String? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val receptorActualizacionConfig = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MainActivity.ACCION_ACTUALIZAR_CONFIGURACION) {
                Log.i(TAG_LOG, "Se recibió una notificación para actualizar la configuración.")
                cargarConfiguracion()
            }
        }
    }

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
        Log.i(TAG_LOG, "Servicio configurado para escuchar eventos de: ${infoServicio.packageNames.joinToString()}")

        // Registrar el BroadcastReceiver
        val filtro = IntentFilter(MainActivity.ACCION_ACTUALIZAR_CONFIGURACION)
        registerReceiver(receptorActualizacionConfig, filtro, RECEIVER_NOT_EXPORTED)
        Log.i(TAG_LOG, "Receptor de actualizaciones de configuración registrado.")
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
        gananciaMinimaConfigurada = prefs.getFloat(
            MainActivity.CLAVE_GANANCIA_MINIMA_VIAJE,
            MainActivity.VALOR_POR_DEFECTO_GANANCIA_MINIMA_VIAJE
        )
        Log.i(
            TAG_LOG,
            "Configuración cargada: Dist. Recogida < $distanciaMaximaKmConfigurada km, Dist. Viaje A-B < $distanciaMaximaViajeABKmConfigurada km, Tarifa: $gananciaPorKmDeseada/km, Ganancia Mínima: $gananciaMinimaConfigurada"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        unregisterReceiver(receptorActualizacionConfig)
        Log.i(TAG_LOG, "Servicio destruido y receptor de actualizaciones desregistrado.")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val nodoRaizOriginal = rootInActiveWindow ?: return
        val nodoRaiz = AccessibilityNodeInfo.obtain(nodoRaizOriginal)

        try {
            when (estadoActual) {
                EstadoDelServicio.BUSCANDO_EN_LISTA -> gestionarEstadoBuscandoEnLista(evento, nodoRaiz)
                EstadoDelServicio.ESPERANDO_APARICION_DETALLE -> gestionarEstadoEsperandoDetalle(evento, nodoRaiz)
                EstadoDelServicio.EN_DETALLE_REVELANDO -> gestionarEstadoRevelandoDetalle(nodoRaiz)
                EstadoDelServicio.EN_DETALLE_PROCESANDO -> gestionarEstadoProcesandoDetalle(evento, nodoRaiz)
                EstadoDelServicio.ESPERANDO_DIALOGO_TARIFA -> gestionarEstadoEsperandoDialogo(evento, nodoRaiz)
                EstadoDelServicio.EN_DIALOGO_TARIFA -> { /* No hacer nada aquí, espera la coroutine */ }
            }
        } finally {
            nodoRaiz.recycle()
        }
    }

    private fun gestionarEstadoBuscandoEnLista(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            serviceScope.launch {
                val nodoCopia = AccessibilityNodeInfo.obtain(nodoRaiz)
                var nodoParaClic: AccessibilityNodeInfo? = null
                try {
                    nodoParaClic = ListaViajesProcessor.encontrarViajeParaAceptar(
                        nodoCopia,
                        distanciaMaximaKmConfigurada,
                        distanciaMaximaViajeABKmConfigurada
                    )

                    if (nodoParaClic != null) {
                        if (intentarHacerClic(nodoParaClic)) {
                            Log.i(TAG_LOG, "==> [SERVICIO] Clic realizado con éxito. Transicionando a ESPERANDO_APARICION_DETALLE.")
                            estadoActual = EstadoDelServicio.ESPERANDO_APARICION_DETALLE
                        } else {
                            Log.w(TAG_LOG, "==> [SERVICIO] Se encontró un viaje, pero el clic falló.")
                        }
                    }
                } finally {
                    nodoParaClic?.recycle()
                    nodoCopia.recycle()
                }
            }
        }
    }

    private fun gestionarEstadoEsperandoDetalle(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (esLaPantallaDeDetalle(nodoRaiz)) {
                Log.i(TAG_LOG, "==> PANTALLA DE DETALLE VERIFICADA. Transicionando a EN_DETALLE_REVELANDO.")
                estadoActual = EstadoDelServicio.EN_DETALLE_REVELANDO
            }
        }
    }

    private fun gestionarEstadoRevelandoDetalle(nodoRaiz: AccessibilityNodeInfo) {
        Log.d(TAG_LOG, "[DETALLE_REVELAR] Buscando imagen clicable para revelar contenido...")
        if (encontrarYClicarImagenParaRevelar(nodoRaiz)) {
            Log.i(TAG_LOG, "[DETALLE_REVELAR] Clic en imagen realizado. Transicionando a EN_DETALLE_PROCESANDO.")
            estadoActual = EstadoDelServicio.EN_DETALLE_PROCESANDO
        } else {
            Log.w(TAG_LOG, "[DETALLE_REVELAR] No se encontró imagen clicable. Se intentará procesar de todas formas.")
            estadoActual = EstadoDelServicio.EN_DETALLE_PROCESANDO
        }
    }

    private fun encontrarYClicarImagenParaRevelar(nodoRaiz: AccessibilityNodeInfo): Boolean {
        var botonCancelar: AccessibilityNodeInfo? = null
        val imagenesClicables = mutableListOf<AccessibilityNodeInfo>()

        // Paso 1: Recorrer el árbol para encontrar el botón de cancelar y todas las imágenes clicables.
        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        while(cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            if (nodo.className == "android.widget.Button" && nodo.text?.toString()?.contains("cancelar", true) == true) {
                if (botonCancelar == null) botonCancelar = AccessibilityNodeInfo.obtain(nodo)
            } else if (nodo.className == "android.widget.ImageView" && nodo.isClickable) {
                imagenesClicables.add(AccessibilityNodeInfo.obtain(nodo))
            }

            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }
        }

        // Paso 2: Si no hay botón de cancelar, no podemos continuar con esta lógica.
        val rectBotonCancelar = Rect()
        if (botonCancelar != null) {
            botonCancelar.getBoundsInScreen(rectBotonCancelar)
            botonCancelar.recycle()
        } else {
            Log.w(TAG_LOG, "[DETALLE_REVELAR] No se encontró el botón 'Cancelar'. No se puede determinar qué imagen clicar.")
            imagenesClicables.forEach { it.recycle() }
            return false
        }

        // Paso 3: Encontrar la imagen clicable más baja que esté por encima del botón de cancelar.
        var imagenObjetivo: AccessibilityNodeInfo? = null
        var maxTop = -1

        imagenesClicables.forEach { imagen ->
            val rectImagen = Rect()
            imagen.getBoundsInScreen(rectImagen)
            // La imagen debe estar por encima del botón
            if (rectImagen.bottom < rectBotonCancelar.top) {
                if (rectImagen.top > maxTop) {
                    maxTop = rectImagen.top
                    imagenObjetivo?.recycle() // Reciclar el candidato anterior
                    imagenObjetivo = AccessibilityNodeInfo.obtain(imagen) // Guardar el nuevo mejor candidato
                }
            }
        }

        // Limpiar la lista de imágenes
        imagenesClicables.forEach { it.recycle() }

        // Paso 4: Hacer clic en la imagen objetivo si se encontró.
        if (imagenObjetivo != null) {
            Log.i(TAG_LOG, "[DETALLE_REVELAR] Imagen objetivo encontrada. Intentando clic.")
            val exito = intentarHacerClic(imagenObjetivo)
            imagenObjetivo.recycle()
            return exito
        } else {
            Log.w(TAG_LOG, "[DETALLE_REVELAR] No se encontró una imagen clicable adecuada por encima del botón Cancelar.")
            return false
        }
    }

    private fun gestionarEstadoProcesandoDetalle(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (!esLaPantallaDeDetalle(nodoRaiz)) {
            Log.i(TAG_LOG, "[DETALLE] La pantalla de detalle ya no es visible o cambió. Volviendo a BUSCANDO_EN_LISTA.")
            estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            return
        }

        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(TAG_LOG, "[DETALLE_PROCESANDO] Procesando evento de cambio de contenido.")
            val infoViaje = DetalleViajeParser.parsear(nodoRaiz)

            Log.i(TAG_LOG, "--- INICIO CÁLCULO DE VIAJE ---")
            Log.i(TAG_LOG, "[INFO] Origen: ${infoViaje.origen ?: "No disponible"}")
            Log.i(TAG_LOG, "[INFO] Destino: ${infoViaje.destino ?: "No disponible"}")
            Log.i(TAG_LOG, "[INFO] Precio Sugerido (UI): ${infoViaje.precioSugeridoNumerico ?: "No disponible"}")
            Log.i(TAG_LOG, "[INFO] Tiempo Estimado (UI): ${infoViaje.tiempoEstimadoMinutos?.toString() ?: "No disponible"} min")
            Log.i(TAG_LOG, "[INFO] Distancia Estimada (UI): ${infoViaje.distanciaEstimadaKm?.toString() ?: "No disponible"} km")
            Log.i(TAG_LOG, "[API] Distancia REAL (API): ${infoViaje.distanciaViajeRealKm?.toString() ?: "No disponible"} km")

            if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico < gananciaMinimaConfigurada) {
                Log.w(TAG_LOG, "[DECISIÓN] Viaje descartado. La ganancia (${infoViaje.precioSugeridoNumerico}) es menor a la mínima configurada (${gananciaMinimaConfigurada}).")
                estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                return
            }

            val distanciaParaCalculo = infoViaje.distanciaViajeRealKm ?: infoViaje.distanciaEstimadaKm

            if (distanciaParaCalculo != null && distanciaParaCalculo > 0) {
                val precioMinimoCalculado = distanciaParaCalculo * gananciaPorKmDeseada
                Log.i(TAG_LOG, "[CÁLCULO] Precio mínimo aceptable (basado en distancia de ${if (infoViaje.distanciaViajeRealKm != null) "API" else "UI"}): $precioMinimoCalculado")

                    // if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico >= precioMinimoCalculado) {
                    //     Log.i(TAG_LOG, "[DECISIÓN] Aceptando precio sugerido (es >= al mínimo calculado).")
                    //     infoViaje.botones.nodoBotonAceptar?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    //     infoViaje.botones.nodoBotonAceptar?.recycle()
                    //     estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                    // } else {
                        val contraoferta = (Math.ceil(precioMinimoCalculado / 500.0) * 500).toInt().toString()
                        Log.i(TAG_LOG, "[DECISIÓN] Contraofertando (calculado): $contraoferta")
                        intentarContraoferta(infoViaje, contraoferta)
                    // }
            } else {
                Log.w(TAG_LOG, "[DETALLE] No se pudo obtener la distancia del viaje (ni de API ni de UI). Volviendo a buscar.")
                estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            }
            Log.i(TAG_LOG, "--- FIN CÁLCULO DE VIAJE ---")
        }
    }

    private fun intentarContraoferta(infoViaje: DetalleViaje, tarifa: String) {
        this.tarifaACalcular = tarifa
        Log.i(TAG_LOG, "[DECISIÓN] Preparando contraoferta: $tarifa")

        infoViaje.botones.nodoBotonEditar?.let {
            Log.d(TAG_LOG, "[DETALLE] Intentando clic en botón Editar para contraofertar.")
            if (intentarHacerClic(it)) {
                estadoActual = EstadoDelServicio.ESPERANDO_DIALOGO_TARIFA
            } else {
                Log.w(TAG_LOG, "[DETALLE] El clic en el botón Editar para contraofertar falló.")
                estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
            }
            it.recycle()
        } ?: run {
            Log.w(TAG_LOG, "[DETALLE] No se encontró el botón Editar para contraofertar. Volviendo a buscar.")
            estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
        }
    }

    private fun gestionarEstadoEsperandoDialogo(evento: AccessibilityEvent?, nodoRaiz: AccessibilityNodeInfo) {
        if (evento?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || evento?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.i(TAG_LOG, "[DIALOGO] Detectado cambio de ventana/contenido. Posible aparición de diálogo de tarifa.")
            estadoActual = EstadoDelServicio.EN_DIALOGO_TARIFA
            serviceScope.launch {
                delay(500L)
                val dialogoNodoRaiz = rootInActiveWindow
                if (dialogoNodoRaiz != null) {
                    gestionarEstadoEnDialogo(dialogoNodoRaiz)
                    dialogoNodoRaiz.recycle()
                } else {
                    Log.w(TAG_LOG, "[DIALOGO] No se pudo obtener el nodo raíz para el diálogo de tarifa. Volviendo a buscar.")
                    estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
                }
            }
        }
    }

    private fun gestionarEstadoEnDialogo(nodoRaizDialogo: AccessibilityNodeInfo) {
        if (tarifaACalcular != null) {
            Log.i(TAG_LOG, "[DIÁLOGO] Intentando ofertar tarifa calculada: $tarifaACalcular")
            val exito = DialogoTarifaProcessor.ofertarNuevaTarifa(nodoRaizDialogo, tarifaACalcular!!)
            Log.i(TAG_LOG, "[DIÁLOGO] Resultado del intento de oferta: ${if(exito) "Éxito" else "Fallo"}")
            tarifaACalcular = null
        } else {
            Log.w(TAG_LOG, "[DIÁLOGO] Se entró a gestionar diálogo sin tarifa a calcular.")
        }
        Log.i(TAG_LOG, "==> Interacción con diálogo finalizada. Volviendo a BUSCANDO_EN_LISTA.")
        estadoActual = EstadoDelServicio.BUSCANDO_EN_LISTA
    }

    private fun esLaPantallaDeDetalle(nodoRaiz: AccessibilityNodeInfo): Boolean {
        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        var esDetalle = false
        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            nodo.text?.toString()?.let { texto ->
                if (TEXTOS_CLAVE_PANTALLA_DETALLE.any { clave -> texto.contains(clave, ignoreCase = true) }) {
                    esDetalle = true
                }
            }
            if (esDetalle) break

            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }
        }
        return esDetalle
    }

    private fun intentarHacerClic(nodo: AccessibilityNodeInfo): Boolean {
        var nodoClicable: AccessibilityNodeInfo? = nodo
        var intentos = 0
        while (nodoClicable != null && intentos < 7) {
            if (nodoClicable.isClickable && nodoClicable.isVisibleToUser && nodoClicable.isEnabled) {
                val resultado = nodoClicable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG_LOG, "[CLIC] performAction resultado: $resultado para '${nodoClicable.text}'")
                return resultado
            }
            val padre = nodoClicable.parent
            if (nodoClicable != nodo) {
                // Si estamos subiendo, no reciclamos el nodo original
            }
            nodoClicable = padre
            intentos++
        }
        Log.w(TAG_LOG, "No se encontró ningún ancestro clicable, visible y habilitado para el nodo original '${nodo.text}'.")
        return false
    }

    private fun hacerClicPorGesto(nodo: AccessibilityNodeInfo) {
        val rect = Rect()
        nodo.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) {
            Log.w(TAG_LOG, "[GESTO] No se puede hacer clic por gesto, el nodo no tiene dimensiones válidas en pantalla: ${nodo.className}")
            return
        }
        val path = Path().apply { moveTo(rect.centerX().toFloat(), rect.centerY().toFloat()) }
        val gesto = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50L)).build()
        Log.d(TAG_LOG, "[GESTO] Enviando gesto de clic a X=${rect.centerX()}, Y=${rect.centerY()} para nodo: ${nodo.className}")
        dispatchGesture(gesto, null, null)
    }

    override fun onInterrupt() {
        Log.w(TAG_LOG, "Servicio de accesibilidad interrumpido.")
        // Aquí puedes manejar la interrupción, por ejemplo, cancelando tareas en segundo plano.
        serviceJob.cancel()
    }
}
