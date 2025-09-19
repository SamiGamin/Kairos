package com.kairos.ast.ui.bienbenida

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kairos.ast.MainActivity
import com.kairos.ast.R
import com.kairos.ast.databinding.DisenoActividadBienvenidaBinding
import com.kairos.ast.servicios.ServicioAccesibilidad

class ActividadBienvenida : AppCompatActivity() {

    // Declaración de la variable de ViewBinding
    private lateinit var binding: DisenoActividadBienvenidaBinding

    // Tag para Logcat, específico para esta actividad
    private val TAG_LOGCAT_BIENVENIDA = "KairosBienvenida"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el layout usando ViewBinding
        binding = DisenoActividadBienvenidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarListeners()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG_LOGCAT_BIENVENIDA, "onResume: Verificando estado del servicio de accesibilidad.")
        verificarEstadoServicioAccesibilidad()
    }

    private fun configurarListeners() {
        binding.botonIrAAjustesAccesibilidad.setOnClickListener {
            abrirAjustesAccesibilidad()
        }

        binding.botonContinuarAPrincipal.setOnClickListener {
            Toast.makeText(this, "Continuando a la funcionalidad principal...", Toast.LENGTH_SHORT).show()
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
            finish()
        }
    }

    private fun abrirAjustesAccesibilidad() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Por favor, busca y activa el servicio \"Kairos\".", Toast.LENGTH_LONG).show()
    }

    private fun verificarEstadoServicioAccesibilidad() {
        if (estaServicioAccesibilidadActivado(this)) {
            Log.i(TAG_LOGCAT_BIENVENIDA, "Servicio de Accesibilidad Kairos VERIFICADO como ACTIVO.")
            binding.botonIrAAjustesAccesibilidad.isEnabled = false
            binding.botonIrAAjustesAccesibilidad.text = "Servicio de Accesibilidad Activado"
            binding.botonContinuarAPrincipal.visibility = View.VISIBLE
        } else {
            Log.w(TAG_LOGCAT_BIENVENIDA, "Servicio de Accesibilidad Kairos VERIFICADO como INACTIVO.")
            binding.botonIrAAjustesAccesibilidad.isEnabled = true
            binding.botonIrAAjustesAccesibilidad.text = getString(R.string.boton_activar_servicio_accesibilidad)
            binding.botonContinuarAPrincipal.visibility = View.GONE
        }
    }

    private fun estaServicioAccesibilidadActivado(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val listaServiciosHabilitados = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        val nuestroPaquetePrincipal = context.packageName // Ej: com.kairos.ast
        val nuestraClaseServicioCompleta = ServicioAccesibilidad::class.java.name // Ej: com.kairos.ast.servicios.ServicioAccesibilidad
        Log.d(TAG_LOGCAT_BIENVENIDA, "Buscando servicio con Paquete Principal: '${nuestroPaquetePrincipal}', Clase Completa Esperada: '${nuestraClaseServicioCompleta}'")

        if (listaServiciosHabilitados.isNullOrEmpty()) {
            Log.w(TAG_LOGCAT_BIENVENIDA, "No hay servicios de accesibilidad habilitados en el sistema.")
            return false
        }

        Log.d(TAG_LOGCAT_BIENVENIDA, "Servicios de accesibilidad HABILITADOS actualmente en el sistema (${listaServiciosHabilitados.size}):")
        for (servicioHabilitadoInfo in listaServiciosHabilitados) {
            val idServicioSistema = servicioHabilitadoInfo.id
            if (idServicioSistema.isNullOrEmpty()) {
                Log.w(TAG_LOGCAT_BIENVENIDA, " - Se encontró un servicio habilitado con ID nulo o vacío.")
                continue
            }

            // Crear ComponentName desde el ID que provee el sistema
            val componenteServicioSistema = ComponentName.unflattenFromString(idServicioSistema)
            if (componenteServicioSistema == null) {
                Log.w(TAG_LOGCAT_BIENVENIDA, " - No se pudo crear ComponentName desde ID del sistema: '${idServicioSistema}'")
                continue
            }

            val paqueteDetectadoSistema = componenteServicioSistema.packageName
            val claseDetectadaSistema = componenteServicioSistema.className // Puede ser relativa o absoluta

            Log.d(TAG_LOGCAT_BIENVENIDA, " - Servicio del sistema detectado: Paquete='${paqueteDetectadoSistema}', Clase='${claseDetectadaSistema}' (ID Original del sistema: '${idServicioSistema}')")

            // Comparar paquetes
            if (nuestroPaquetePrincipal.equals(paqueteDetectadoSistema, ignoreCase = true)) {
                // Si la clase detectada es relativa (comienza con '.'), la hacemos absoluta usando el paquete detectado
                val claseNormalizadaSistema: String
                if (claseDetectadaSistema.startsWith(".")) {
                    claseNormalizadaSistema = paqueteDetectadoSistema + claseDetectadaSistema
                    Log.d(TAG_LOGCAT_BIENVENIDA, "   Clase del sistema normalizada a absoluta: '${claseNormalizadaSistema}'")
                } else {
                    claseNormalizadaSistema = claseDetectadaSistema
                }

                // Comparar la clase de nuestro servicio (siempre absoluta) con la clase normalizada del sistema (ahora también absoluta)
                if (nuestraClaseServicioCompleta.equals(claseNormalizadaSistema, ignoreCase = true)) {
                    Log.i(TAG_LOGCAT_BIENVENIDA, "¡Coincidencia DEFINITIVA encontrada! Servicio Kairos está activo. Paquete='${paqueteDetectadoSistema}', Clase Normalizada='${claseNormalizadaSistema}'")
                    return true
                } else {
                    Log.d(TAG_LOGCAT_BIENVENIDA, "   Clases no coinciden: Esperada='${nuestraClaseServicioCompleta}', Sistema Normalizada='${claseNormalizadaSistema}'")
                }
            } else {
                 Log.d(TAG_LOGCAT_BIENVENIDA, "   Paquetes no coinciden: Esperado='${nuestroPaquetePrincipal}', Sistema='${paqueteDetectadoSistema}'")
            }
        }
        Log.w(TAG_LOGCAT_BIENVENIDA, "No se encontró el servicio Kairos (Paquete='${nuestroPaquetePrincipal}', Clase Completa='${nuestraClaseServicioCompleta}') en la lista de servicios habilitados.")
        return false
    }
}