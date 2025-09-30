package com.kairos.ast.ui.Permisos

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kairos.ast.MainViewModel
import com.kairos.ast.R
import com.kairos.ast.databinding.DisenoActividadBienvenidaBinding
import com.kairos.ast.servicios.ServicioAccesibilidad

class PermisosFragment : Fragment() {

    private var _binding: DisenoActividadBienvenidaBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
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
        mainViewModel.actualizarEstadoUsuario()
        configurarListeners()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadosPermisos()
    }

    private fun configurarListeners() {
        binding.botonIrAAjustesAccesibilidad.setOnClickListener {
            abrirAjustesAccesibilidad()
        }
        binding.botonIrAAjustesBateria.setOnClickListener {
            abrirAjustesOptimizacionBateria()
        }
        binding.botonContinuarAPrincipal.setOnClickListener {
            findNavController().navigate(R.id.action_permisosFragment_to_fragmentoIndrive)
        }
        binding.botonIrAAjustesAutoinicio.setOnClickListener {
            abrirAjustesAutoinicio(context = requireContext())
        }

    }
    private fun abrirAjustesAutoinicio(context: Context) {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(Uri.parse("mobilemanager://function/entry/AutoStart"))
        )

        var didSucceed = false
        for (intent in intents) {
            try {
                context.startActivity(intent)
                didSucceed = true
                break
            } catch (e: Exception) {
                // El intent no funcionó en este dispositivo
            }
        }

        if (!didSucceed) {
            Toast.makeText(context, "No se pudo abrir la pantalla de auto-inicio automáticamente. Búscala manualmente en los ajustes de tu dispositivo.", Toast.LENGTH_LONG).show()
        }
    }

    private fun actualizarEstadosPermisos() {
        val accesibilidadOk = verificarEstadoServicioAccesibilidad()
        val bateriaOk = verificarEstadoOptimizacionBateria()

        if (accesibilidadOk && bateriaOk) {
            binding.botonContinuarAPrincipal.visibility = View.VISIBLE
        } else {
            binding.botonContinuarAPrincipal.visibility = View.GONE
        }
    }

    private fun abrirAjustesAccesibilidad() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(requireContext(), "Por favor, busca y activa el servicio \"Kairos\".", Toast.LENGTH_LONG).show()
    }

    private fun abrirAjustesOptimizacionBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${requireContext().packageName}")
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Función no disponible en esta versión de Android.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarEstadoServicioAccesibilidad(): Boolean {
        val activado = estaServicioAccesibilidadActivado(requireContext())
        if (activado) {
            binding.textoEstadoPermiso.text = "Estado: Activado"
            binding.textoEstadoPermiso.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary))
            binding.botonIrAAjustesAccesibilidad.isEnabled = false
            binding.botonIrAAjustesAccesibilidad.text = "Servicio Activado"
            binding.textoExplicacionAdicionalPermisos.visibility = View.GONE
        } else {
            binding.textoEstadoPermiso.text = "Estado: Pendiente"
            binding.textoEstadoPermiso.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_error))
            binding.botonIrAAjustesAccesibilidad.isEnabled = true
            binding.botonIrAAjustesAccesibilidad.text = getString(R.string.boton_activar_servicio_accesibilidad)
            binding.textoExplicacionAdicionalPermisos.visibility = View.VISIBLE
        }
        return activado
    }

    private fun verificarEstadoOptimizacionBateria(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = requireContext().packageName
            val sinRestricciones = powerManager.isIgnoringBatteryOptimizations(packageName)

            if (sinRestricciones) {
                binding.cardBateria.visibility = View.VISIBLE
                binding.textoEstadoBateria.text = "Estado: Optimización activada"
                binding.textoEstadoBateria.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary))
                binding.botonIrAAjustesBateria.isEnabled = false
            } else {
                binding.cardBateria.visibility = View.VISIBLE
                binding.textoEstadoBateria.text =  "Estado: Optimización desactivada"
                binding.textoEstadoBateria.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_error))
                binding.botonIrAAjustesBateria.isEnabled = true
            }
            return sinRestricciones
        }
        return true // Para versiones antiguas de Android, asumimos que no hay problema.
    }

    private fun estaServicioAccesibilidadActivado(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val listaServiciosHabilitados = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        // Forma 1: Nombre completo (oficial)
        val componentNameOficial = ComponentName(context, ServicioAccesibilidad::class.java)
        val idOficial = componentNameOficial.flattenToString()

        // Forma 2: Nombre abreviado (visto en algunos dispositivos)
        val shortClassName = ServicioAccesibilidad::class.java.name.substring(context.packageName.length)
        val idAlternativo = context.packageName + "/" + shortClassName

        Log.d(TAG_LOGCAT_PERMISOS, "Buscando servicio con ID Oficial: $idOficial")
        Log.d(TAG_LOGCAT_PERMISOS, "Buscando servicio con ID Alternativo: $idAlternativo")
        Log.d(TAG_LOGCAT_PERMISOS, "Lista de servicios de accesibilidad ACTIVOS:")
        if (listaServiciosHabilitados.isEmpty()) {
            Log.d(TAG_LOGCAT_PERMISOS, "- (Ninguno)")
        }

        for (servicioHabilitado in listaServiciosHabilitados) {
            val idServicioActual = servicioHabilitado.id
            Log.d(TAG_LOGCAT_PERMISOS, "- ID: $idServicioActual")
            if (idServicioActual.equals(idOficial, ignoreCase = true) || idServicioActual.equals(idAlternativo, ignoreCase = true)) {
                Log.i(TAG_LOGCAT_PERMISOS, "¡Servicio ENCONTRADO!")
                return true
            }
        }

        Log.w(TAG_LOGCAT_PERMISOS, "Servicio NO encontrado en la lista de activos.")
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
