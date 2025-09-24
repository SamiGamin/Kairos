package com.kairos.ast.ui.Permisos

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kairos.ast.R
import com.kairos.ast.databinding.DisenoActividadBienvenidaBinding
import com.kairos.ast.servicios.ServicioAccesibilidad

class PermisosFragment : Fragment() {

    private var _binding: DisenoActividadBienvenidaBinding? = null
    private val binding get() = _binding!!

    private val TAG_LOGCAT_PERMISOS = "KairosPermisosFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DisenoActividadBienvenidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarListeners()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG_LOGCAT_PERMISOS, "onResume: Verificando estado del servicio de accesibilidad.")
        verificarEstadoServicioAccesibilidad()
    }

    private fun configurarListeners() {
        binding.botonIrAAjustesAccesibilidad.setOnClickListener {
            abrirAjustesAccesibilidad()
        }

        binding.botonContinuarAPrincipal.setOnClickListener {
            findNavController().navigate(R.id.action_permisosFragment_to_fragmentoIndrive)
        }
    }

    private fun abrirAjustesAccesibilidad() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(requireContext(), "Por favor, busca y activa el servicio \"Kairos\".", Toast.LENGTH_LONG).show()
    }

    private fun verificarEstadoServicioAccesibilidad() {
        if (estaServicioAccesibilidadActivado(requireContext())) {
            Log.i(TAG_LOGCAT_PERMISOS, "Servicio de Accesibilidad Kairos VERIFICADO como ACTIVO.")
            binding.botonIrAAjustesAccesibilidad.isEnabled = false
            binding.botonIrAAjustesAccesibilidad.text = "Servicio de Accesibilidad Activado"
            binding.botonContinuarAPrincipal.visibility = View.VISIBLE
        } else {
            Log.w(TAG_LOGCAT_PERMISOS, "Servicio de Accesibilidad Kairos VERIFICADO como INACTIVO.")
            binding.botonIrAAjustesAccesibilidad.isEnabled = true
            binding.botonIrAAjustesAccesibilidad.text = getString(R.string.boton_activar_servicio_accesibilidad)
            binding.botonContinuarAPrincipal.visibility = View.GONE
        }
    }

    private fun estaServicioAccesibilidadActivado(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val listaServiciosHabilitados = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        val nuestroPaquetePrincipal = context.packageName
        val nuestraClaseServicioCompleta = ServicioAccesibilidad::class.java.name
        Log.d(TAG_LOGCAT_PERMISOS, "Buscando servicio con Paquete Principal: '${nuestroPaquetePrincipal}', Clase Completa Esperada: '${nuestraClaseServicioCompleta}'")

        if (listaServiciosHabilitados.isNullOrEmpty()) {
            Log.w(TAG_LOGCAT_PERMISOS, "No hay servicios de accesibilidad habilitados en el sistema.")
            return false
        }

        Log.d(TAG_LOGCAT_PERMISOS, "Servicios de accesibilidad HABILITADOS actualmente en el sistema (${listaServiciosHabilitados.size}):")
        for (servicioHabilitadoInfo in listaServiciosHabilitados) {
            val idServicioSistema = servicioHabilitadoInfo.id
            if (idServicioSistema.isNullOrEmpty()) {
                Log.w(TAG_LOGCAT_PERMISOS, " - Se encontró un servicio habilitado con ID nulo o vacío.")
                continue
            }

            val componenteServicioSistema = ComponentName.unflattenFromString(idServicioSistema)
            if (componenteServicioSistema == null) {
                Log.w(TAG_LOGCAT_PERMISOS, " - No se pudo crear ComponentName desde ID del sistema: '${idServicioSistema}'")
                continue
            }

            val paqueteDetectadoSistema = componenteServicioSistema.packageName
            val claseDetectadaSistema = componenteServicioSistema.className

            Log.d(TAG_LOGCAT_PERMISOS, " - Servicio del sistema detectado: Paquete='${paqueteDetectadoSistema}', Clase='${claseDetectadaSistema}' (ID Original del sistema: '${idServicioSistema}')")

            if (nuestroPaquetePrincipal.equals(paqueteDetectadoSistema, ignoreCase = true)) {
                val claseNormalizadaSistema: String
                if (claseDetectadaSistema.startsWith(".")) {
                    claseNormalizadaSistema = paqueteDetectadoSistema + claseDetectadaSistema
                    Log.d(TAG_LOGCAT_PERMISOS, "   Clase del sistema normalizada a absoluta: '${claseNormalizadaSistema}'")
                } else {
                    claseNormalizadaSistema = claseDetectadaSistema
                }

                if (nuestraClaseServicioCompleta.equals(claseNormalizadaSistema, ignoreCase = true)) {
                    Log.i(TAG_LOGCAT_PERMISOS, "¡Coincidencia DEFINITIVA encontrada! Servicio Kairos está activo. Paquete='${paqueteDetectadoSistema}', Clase Normalizada='${claseNormalizadaSistema}'")
                    return true
                } else {
                    Log.d(TAG_LOGCAT_PERMISOS, "   Clases no coinciden: Esperada='${nuestraClaseServicioCompleta}', Sistema Normalizada='${claseNormalizadaSistema}'")
                }
            } else {
                 Log.d(TAG_LOGCAT_PERMISOS, "   Paquetes no coinciden: Esperado='${nuestroPaquetePrincipal}', Sistema='${paqueteDetectadoSistema}'")
            }
        }
        Log.d(TAG_LOGCAT_PERMISOS, "No se encontró el servicio Kairos (Paquete='${nuestroPaquetePrincipal}', Clase Completa='${nuestraClaseServicioCompleta}') en la lista de servicios habilitados.")
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
