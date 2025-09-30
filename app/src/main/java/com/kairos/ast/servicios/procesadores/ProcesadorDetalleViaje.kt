package com.kairos.ast.servicios.procesadores

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.configuracion.ConfiguracionServicio
import com.kairos.ast.servicios.detalles.DetalleViaje
import com.kairos.ast.servicios.detalles.DetalleViajeParser
import com.kairos.ast.servicios.estado.EstadoServicio
import com.kairos.ast.servicios.utils.intentarClic

private const val TAG_LOG = "ProcesadorDetalleViaje"

/**
 * Objeto responsable de procesar la pantalla de detalle de un viaje.
 * Su lógica principal es parsear la información, compararla con la configuración
 * del usuario y decidir si el viaje se acepta, se contraoferta o se ignora.
 */
object ProcesadorDetalleViaje {

    /**
     * Procesa la pantalla de detalle.
     *
     * @param nodoRaiz El nodo raíz de la pantalla de detalle.
     * @param configuracion La configuración actual del servicio.
     * @param onContraoferta Callback que se invoca con la tarifa a contraofertar.
     * @return El siguiente estado al que debe transicionar el servicio.
     */
    fun procesar(
        nodoRaiz: AccessibilityNodeInfo,
        configuracion: ConfiguracionServicio,
        onContraoferta: (String) -> Unit
    ): EstadoServicio {
        val infoViaje = DetalleViajeParser.parsear(nodoRaiz)

        Log.i(TAG_LOG, "--- INICIO CÁLCULO DE VIAJE ---")
        Log.i(TAG_LOG, "[INFO] Origen: ${infoViaje.origen ?: "No disponible"}")
        Log.i(TAG_LOG, "[INFO] Destino: ${infoViaje.destino ?: "No disponible"}")
        Log.i(TAG_LOG, "[INFO] Precio Sugerido (UI): ${infoViaje.precioSugeridoNumerico ?: "No disponible"}")
        Log.i(TAG_LOG, "[API] Distancia REAL (API): ${infoViaje.distanciaViajeRealKm?.toString() ?: "No disponible"} km")
        Log.i(TAG_LOG, "---------------------------------")

        val distanciaDelViaje = infoViaje.distanciaViajeRealKm ?: infoViaje.distanciaEstimadaKm

        // Si las acciones automáticas están desactivadas, solo analiza y sigue buscando.
        if (!configuracion.accionesAutomaticas) {
            analizarYLoguearSinActuar(infoViaje, configuracion, distanciaDelViaje)
            return EstadoServicio.BuscandoEnLista
        }

        // Criterio 1: Manejar viajes con ganancia menor a la mínima.
        if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico < configuracion.gananciaMinimaViaje) {
            // Criterio especial: si el viaje es corto (< 5km), contraofertar con la ganancia mínima.
            if (distanciaDelViaje != null && distanciaDelViaje < 5.0f) {
                Log.i(TAG_LOG, "[DECISIÓN] Viaje con ganancia baja pero es < 5km. Contraofertando con la ganancia mínima.")
                val contraofertaMinima = (Math.ceil(configuracion.gananciaMinimaViaje / 500.0) * 500).toInt().toString()
                return intentarContraoferta(infoViaje, contraofertaMinima, onContraoferta)
            } else {
                // Si no cumple el criterio especial, se descarta.
                Log.w(
                    TAG_LOG,
                    "[DECISIÓN] Viaje descartado. La ganancia (${infoViaje.precioSugeridoNumerico}) es menor a la mínima configurada (${configuracion.gananciaMinimaViaje}) y el viaje no es corto."
                )
                return EstadoServicio.BuscandoEnLista
            }
        }

        // Criterio 2: Calcular precio mínimo para viajes con ganancia aceptable o sin precio sugerido.
        if (distanciaDelViaje != null && distanciaDelViaje > 0) {
            val precioMinimoCalculado = distanciaDelViaje * configuracion.gananciaPorKmDeseada
            Log.i(
                TAG_LOG,
                "[CÁLCULO] Precio mínimo aceptable (basado en distancia de ${if (infoViaje.distanciaViajeRealKm != null) "API" else "UI"}): $precioMinimoCalculado"
            )

            if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico >= precioMinimoCalculado) {
                Log.i(TAG_LOG, "[DECISIÓN] PRECIO ACEPTABLE. El sugerido (${infoViaje.precioSugeridoNumerico}) es >= al mínimo (${precioMinimoCalculado}). Aceptando automáticamente.")
                return intentarAceptar(infoViaje)
            } else {
                if (infoViaje.precioSugeridoNumerico != null) {
                    Log.i(TAG_LOG, "[DECISIÓN] CONTRAOFERTAR. El sugerido (${infoViaje.precioSugeridoNumerico}) es < al mínimo (${precioMinimoCalculado}).")
                } else {
                    Log.i(TAG_LOG, "[DECISIÓN] No hay precio sugerido. Contraofertando con el mínimo calculado.")
                }
                
                val contraoferta = (Math.ceil(precioMinimoCalculado / 500.0) * 500).toInt().toString()
                Log.i(TAG_LOG, "[CONTRAOFERTA] Calculando y preparando contraoferta: $contraoferta")
                return intentarContraoferta(infoViaje, contraoferta, onContraoferta)
            }
        } else {
            Log.w(TAG_LOG, "[DETALLE] No se pudo obtener la distancia del viaje (ni de API ni de UI). Volviendo a buscar.")
            return EstadoServicio.BuscandoEnLista
        }
    }

    /**
     * Realiza el mismo análisis que `procesar` pero sin ejecutar acciones, solo registrando en el log
     * la decisión que se habría tomado. Se usa cuando las acciones automáticas están desactivadas.
     */
    private fun analizarYLoguearSinActuar(
        infoViaje: DetalleViaje,
        configuracion: ConfiguracionServicio,
        distanciaDelViaje: Float?
    ) {
        Log.i(TAG_LOG, "[ANÁLISIS - MODO MANUAL]")

        // Criterio 1: Ganancia mínima
        if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico < configuracion.gananciaMinimaViaje) {
            if (distanciaDelViaje != null && distanciaDelViaje < 5.0f) {
                Log.i(TAG_LOG, "[ANÁLISIS] Se habría contraofertado con la ganancia mínima por ser un viaje corto.")
            } else {
                Log.i(TAG_LOG, "[ANÁLISIS] Se habría descartado el viaje por ganancia menor a la mínima.")
            }
            return
        }

        // Criterio 2: Precio mínimo calculado
        if (distanciaDelViaje != null && distanciaDelViaje > 0) {
            val precioMinimoCalculado = distanciaDelViaje * configuracion.gananciaPorKmDeseada
            if (infoViaje.precioSugeridoNumerico != null && infoViaje.precioSugeridoNumerico >= precioMinimoCalculado) {
                Log.i(TAG_LOG, "[ANÁLISIS] Se habría ACEPTADO el viaje automáticamente.")
            } else {
                Log.i(TAG_LOG, "[ANÁLISIS] Se habría CONTRAOFERTADO con el precio mínimo calculado.")
            }
        } else {
            Log.w(TAG_LOG, "[ANÁLISIS] No se pudo obtener la distancia del viaje para tomar una decisión.")
        }
    }

    /**
     * Intenta hacer clic en el botón de Aceptar viaje.
     */
    private fun intentarAceptar(infoViaje: DetalleViaje): EstadoServicio {
        infoViaje.botones.nodoBotonAceptar?.let {
            Log.d(TAG_LOG, "[ACEPTAR] Intentando clic en botón Aceptar.")
            val exito = it.intentarClic()
            it.recycle()

            if (exito) {
                Log.i(TAG_LOG, "[ACEPTAR] Clic en Aceptar exitoso.")
            } else {
                Log.w(TAG_LOG, "[ACEPTAR] El clic en el botón Aceptar falló.")
            }
        } ?: run {
            Log.w(TAG_LOG, "[ACEPTAR] No se encontró el botón Aceptar.")
        }
        return EstadoServicio.BuscandoEnLista // Siempre volver a buscar después de la acción
    }

    /**
     * Intenta hacer clic en el botón de editar para iniciar una contraoferta.
     */
    private fun intentarContraoferta(
        infoViaje: DetalleViaje,
        tarifa: String,
        onContraoferta: (String) -> Unit
    ): EstadoServicio {
        Log.i(TAG_LOG, "[CONTRAOFERTA] Preparando contraoferta: $tarifa")
        onContraoferta(tarifa) // Informar al servicio principal la tarifa a establecer

        infoViaje.botones.nodoBotonEditar?.let { 
            Log.d(TAG_LOG, "[CONTRAOFERTA] Intentando clic en botón Editar.")
            val exito = it.intentarClic()
            it.recycle() // Reciclamos el nodo del botón después de usarlo.

            return if (exito) {
                Log.i(TAG_LOG, "[CONTRAOFERTA] Clic en Editar exitoso. Esperando diálogo.")
                EstadoServicio.EsperandoDialogoTarifa
            } else {
                Log.w(TAG_LOG, "[CONTRAOFERTA] El clic en el botón Editar falló.")
                EstadoServicio.BuscandoEnLista
            }
        } ?: run {
            Log.w(TAG_LOG, "[CONTRAOFERTA] No se encontró el botón Editar. Volviendo a buscar.")
            return EstadoServicio.BuscandoEnLista
        }
    }
}
