package com.kairos.ast.servicios.configuracion

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kairos.ast.MainActivity

private const val TAG_LOG = "ConfiguracionManager"

/**
 * Clase responsable de cargar y mantener actualizada la configuración del servicio.
 * Escucha los cambios en SharedPreferences en tiempo real.
 */
class ConfiguracionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCIAS_APP_KAIROS, Context.MODE_PRIVATE)

    // La configuración actual, accesible públicamente pero solo modificable desde esta clase.
    lateinit var configuracion: ConfiguracionServicio
        private set

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        Log.d(TAG_LOG, "Preferencia cambiada: $key. Recargando configuración.")
        // Para ser más eficientes, podríamos comprobar si la 'key' es una de las que nos interesan.
        cargar()
    }

    /**
     * Realiza la carga inicial de la configuración y registra el listener.
     */
    fun iniciar() {
        cargar()
        prefs.registerOnSharedPreferenceChangeListener(listener)
        Log.i(TAG_LOG, "ConfiguracionManager iniciado y escuchando cambios.")
    }

    /**
     * Libera los recursos y desregistra el listener para evitar memory leaks.
     */
    fun destruir() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        Log.i(TAG_LOG, "ConfiguracionManager destruido.")
    }

    /**
     * Carga la configuración actual desde SharedPreferences.
     * Si algún valor no está definido, utiliza los valores por defecto.
     */
    private fun cargar() {
        configuracion = ConfiguracionServicio(
            distanciaMaximaRecogidaKm = prefs.getFloat(
                MainActivity.CLAVE_DISTANCIA_MAXIMA_KM_LISTA,
                MainActivity.VALOR_POR_DEFECTO_DISTANCIA_KM
            ),
            gananciaPorKmDeseada = prefs.getFloat(
                MainActivity.CLAVE_TARIFA_POR_KM,
                MainActivity.VALOR_POR_DEFECTO_TARIFA_KM
            ),
            distanciaMaximaViajeABKm = prefs.getFloat(
                MainActivity.CLAVE_DISTANCIA_MAXIMA_VIAJE_AB_KM,
                MainActivity.VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM
            ),
            gananciaMinimaViaje = prefs.getFloat(
                MainActivity.CLAVE_GANANCIA_MINIMA_VIAJE,
                MainActivity.VALOR_POR_DEFECTO_GANANCIA_MINIMA_VIAJE
            )
        )

        Log.i(
            TAG_LOG,
            "Configuración (re)cargada: Dist. Recogida <" +
                    " ${configuracion.distanciaMaximaRecogidaKm} km, " +
            "Tarifa: ${configuracion.gananciaPorKmDeseada}/km" +
            ", Dist. Viaje AB < ${configuracion.distanciaMaximaViajeABKm} km" +
            ", Ganancia Mínima Viaje: ${configuracion.gananciaMinimaViaje}"
        )
    }
}