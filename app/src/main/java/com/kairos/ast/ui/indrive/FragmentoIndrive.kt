package com.kairos.ast.ui.indrive

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kairos.ast.MainActivity // Para acceder a las constantes
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentoIndriveBinding

class FragmentoIndrive : Fragment() {

    private var _binding: FragmentoIndriveBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentoIndriveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarConfiguracionGuardada()
        configurarListeners()
    }

    private fun ocultarTeclado() {
        val view = requireActivity().currentFocus
        view?.let {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun cargarConfiguracionGuardada() {
        val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
            MainActivity.PREFERENCIAS_APP_KAIROS,
            Context.MODE_PRIVATE
        )
        // Cargar distancia de recogida
        val distanciaGuardada = preferencias.getFloat(
            MainActivity.CLAVE_DISTANCIA_MAXIMA_KM_LISTA,
            MainActivity.VALOR_POR_DEFECTO_DISTANCIA_KM
        )
        binding.campoDistanciaMaxima.setText(distanciaGuardada.toString())
        actualizarTextoDistanciaGuardada(distanciaGuardada)

        // Cargar tarifa por km
        val tarifaGuardada = preferencias.getFloat(
            MainActivity.CLAVE_TARIFA_POR_KM,
            MainActivity.VALOR_POR_DEFECTO_TARIFA_KM
        )
        binding.campoTarifaKm.setText(tarifaGuardada.toInt().toString())
        actualizarTextoTarifaGuardada(tarifaGuardada)

        // Cargar distancia máxima del viaje A-B
        val distanciaViajeABGuardada = preferencias.getFloat(
            MainActivity.CLAVE_DISTANCIA_MAXIMA_VIAJE_AB_KM,
            MainActivity.VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM
        )
        binding.campoDistanciaMaximaViajeAb.setText(distanciaViajeABGuardada.toString())
        actualizarTextoDistanciaViajeABGuardada(distanciaViajeABGuardada)

        // Cargar ganancia mínima por viaje
        val gananciaMinimaGuardada = preferencias.getFloat(
            MainActivity.CLAVE_GANANCIA_MINIMA_VIAJE,
            MainActivity.VALOR_POR_DEFECTO_GANANCIA_MINIMA_VIAJE
        )
        binding.campoGananciaMinima.setText(gananciaMinimaGuardada.toInt().toString())
        actualizarTextoGananciaMinima(gananciaMinimaGuardada)

        // Cargar filtro de pasajero
        val filtrarPorCalificacion = preferencias.getBoolean(
            MainActivity.CLAVE_FILTRAR_CALIFICACION,
            MainActivity.VALOR_POR_DEFECTO_FILTRAR_CALIFICACION
        )
        val minCalificacion = preferencias.getFloat(
            MainActivity.CLAVE_MIN_CALIFICACION,
            MainActivity.VALOR_POR_DEFECTO_MIN_CALIFICACION
        )
        val minViajes = preferencias.getInt(
            MainActivity.CLAVE_MIN_VIAJES,
            MainActivity.VALOR_POR_DEFECTO_MIN_VIAJES
        )

        binding.switchFiltrarCalificacionViajes.isChecked = filtrarPorCalificacion
        binding.campoCalificacionMinima.setText(minCalificacion.toString())
        binding.campoViajesMinimos.setText(minViajes.toString())

        actualizarEstadoUiFiltroPasajero(filtrarPorCalificacion)
        actualizarTextoFiltroPasajeroGuardado(filtrarPorCalificacion, minCalificacion, minViajes)

        // Cargar acciones automáticas
        val accionesAutomaticas = preferencias.getBoolean(
            MainActivity.CLAVE_ACCIONES_AUTOMATICAS,
            MainActivity.VALOR_POR_DEFECTO_ACCIONES_AUTOMATICAS
        )
        binding.switchAccionesAutomaticas.isChecked = accionesAutomaticas
    }

    private fun configurarListeners() {
        binding.botonGuardarConfigDistancia.setOnClickListener {
            ocultarTeclado()
            guardarConfiguracionDistanciaRecogida()
        }

        binding.botonGuardarTarifa.setOnClickListener {
            ocultarTeclado()
            guardarConfiguracionTarifa()
        }

        binding.botonGuardarConfigDistanciaViajeAb.setOnClickListener {
            ocultarTeclado()
            guardarConfiguracionDistanciaViajeAB()
        }

        binding.botonGuardarGananciaMinima.setOnClickListener {
            ocultarTeclado()
            guardarConfiguracionGananciaMinima()
        }

        binding.botonGuardarFiltroPasajero.setOnClickListener {
            ocultarTeclado()
            guardarConfiguracionFiltroPasajero()
        }

        binding.switchFiltrarCalificacionViajes.setOnCheckedChangeListener { _, isChecked ->
            actualizarEstadoUiFiltroPasajero(isChecked)
        }

        binding.switchAccionesAutomaticas.setOnCheckedChangeListener { _, isChecked ->
            guardarConfiguracionAccionesAutomaticas(isChecked)
        }
    }

    private fun guardarConfiguracionAccionesAutomaticas(isActivado: Boolean) {
        val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
            MainActivity.PREFERENCIAS_APP_KAIROS,
            Context.MODE_PRIVATE
        )
        with(preferencias.edit()) {
            putBoolean(MainActivity.CLAVE_ACCIONES_AUTOMATICAS, isActivado)
            apply()
        }
        enviarNotificacionDeActualizacion()
        val mensaje = if (isActivado) "Acciones automáticas activadas" else "Acciones automáticas desactivadas"
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun guardarConfiguracionDistanciaRecogida() {
        val textoDistancia = binding.campoDistanciaMaxima.text.toString()
        try {
            val nuevaDistancia = textoDistancia.toFloat()
            if (nuevaDistancia <= 0) {
                Toast.makeText(requireContext(), "La distancia de recogida debe ser un valor positivo.", Toast.LENGTH_SHORT).show()
                return
            }

            val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
                MainActivity.PREFERENCIAS_APP_KAIROS,
                Context.MODE_PRIVATE
            )
            with(preferencias.edit()) {
                putFloat(MainActivity.CLAVE_DISTANCIA_MAXIMA_KM_LISTA, nuevaDistancia)
                apply()
            }
            enviarNotificacionDeActualizacion()
            actualizarTextoDistanciaGuardada(nuevaDistancia)
            Toast.makeText(requireContext(), getString(R.string.toast_distancia_guardada, nuevaDistancia), Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_formato_distancia), Toast.LENGTH_LONG).show()
        }
    }

    private fun guardarConfiguracionTarifa() {
        val textoTarifa = binding.campoTarifaKm.text.toString()
        try {
            val nuevaTarifa = textoTarifa.toFloat()
            if (nuevaTarifa <= 0) {
                Toast.makeText(requireContext(), "La tarifa debe ser un valor positivo.", Toast.LENGTH_SHORT).show()
                return
            }

            val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
                MainActivity.PREFERENCIAS_APP_KAIROS,
                Context.MODE_PRIVATE
            )
            with(preferencias.edit()) {
                putFloat(MainActivity.CLAVE_TARIFA_POR_KM, nuevaTarifa)
                apply()
            }
            enviarNotificacionDeActualizacion()
            actualizarTextoTarifaGuardada(nuevaTarifa)
            Toast.makeText(requireContext(), getString(R.string.toast_tarifa_guardada, nuevaTarifa.toInt()), Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_formato_tarifa), Toast.LENGTH_LONG).show()
        }
    }

    private fun guardarConfiguracionDistanciaViajeAB() {
        val textoDistancia = binding.campoDistanciaMaximaViajeAb.text.toString()
        try {
            val nuevaDistancia = textoDistancia.toFloat()
            if (nuevaDistancia <= 0) {
                Toast.makeText(requireContext(), "La distancia máxima del viaje A-B debe ser un valor positivo.", Toast.LENGTH_SHORT).show()
                return
            }

            val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
                MainActivity.PREFERENCIAS_APP_KAIROS,
                Context.MODE_PRIVATE
            )
            with(preferencias.edit()) {
                putFloat(MainActivity.CLAVE_DISTANCIA_MAXIMA_VIAJE_AB_KM, nuevaDistancia)
                apply()
            }
            enviarNotificacionDeActualizacion()
            actualizarTextoDistanciaViajeABGuardada(nuevaDistancia)
            Toast.makeText(requireContext(), getString(R.string.toast_distancia_viaje_ab_guardada, nuevaDistancia), Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_formato_distancia_viaje_ab), Toast.LENGTH_LONG).show()
        }
    }

    private fun guardarConfiguracionGananciaMinima() {
        val textoGanancia = binding.campoGananciaMinima.text.toString()
        try {
            val nuevaGanancia = textoGanancia.toFloat()
            if (nuevaGanancia <= 0) {
                Toast.makeText(requireContext(), "La ganancia mínima debe ser un valor positivo.", Toast.LENGTH_SHORT).show()
                return
            }

            val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
                MainActivity.PREFERENCIAS_APP_KAIROS,
                Context.MODE_PRIVATE
            )
            with(preferencias.edit()) {
                putFloat(MainActivity.CLAVE_GANANCIA_MINIMA_VIAJE, nuevaGanancia)
                apply()
            }
            enviarNotificacionDeActualizacion()
            actualizarTextoGananciaMinima(nuevaGanancia)
            Toast.makeText(requireContext(), getString(R.string.toast_ganancia_minima_guardada, nuevaGanancia.toInt()), Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_formato_ganancia_minima), Toast.LENGTH_LONG).show()
        }
    }

    private fun guardarConfiguracionFiltroPasajero() {
        val isActivado = binding.switchFiltrarCalificacionViajes.isChecked
        val calificacionStr = binding.campoCalificacionMinima.text.toString()
        val viajesStr = binding.campoViajesMinimos.text.toString()

        try {
            val nuevaCalificacion = calificacionStr.toFloatOrNull() ?: MainActivity.VALOR_POR_DEFECTO_MIN_CALIFICACION
            val nuevosViajes = viajesStr.toIntOrNull() ?: MainActivity.VALOR_POR_DEFECTO_MIN_VIAJES

            if (isActivado && (nuevaCalificacion < 0 || nuevaCalificacion > 5)) {
                Toast.makeText(requireContext(), "La calificación debe estar entre 0.0 y 5.0", Toast.LENGTH_SHORT).show()
                return
            }

            if (isActivado && nuevosViajes < 0) {
                Toast.makeText(requireContext(), "El número de viajes no puede ser negativo", Toast.LENGTH_SHORT).show()
                return
            }

            val preferencias: SharedPreferences = requireActivity().getSharedPreferences(
                MainActivity.PREFERENCIAS_APP_KAIROS,
                Context.MODE_PRIVATE
            )
            with(preferencias.edit()) {
                putBoolean(MainActivity.CLAVE_FILTRAR_CALIFICACION, isActivado)
                putBoolean(MainActivity.CLAVE_FILTRAR_NUMERO_VIAJES, isActivado)
                putFloat(MainActivity.CLAVE_MIN_CALIFICACION, nuevaCalificacion)
                putInt(MainActivity.CLAVE_MIN_VIAJES, nuevosViajes)
                apply()
            }
            enviarNotificacionDeActualizacion()
            actualizarTextoFiltroPasajeroGuardado(isActivado, nuevaCalificacion, nuevosViajes)
            Toast.makeText(requireContext(), "Configuración de filtro de pasajero guardada.", Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error de formato en calificación o viajes.", Toast.LENGTH_LONG).show()
        }
    }

    private fun enviarNotificacionDeActualizacion() {
        val intent = Intent(MainActivity.ACCION_ACTUALIZAR_CONFIGURACION)
        requireContext().sendBroadcast(intent)
    }

    private fun actualizarTextoDistanciaGuardada(distancia: Float) {
        binding.textoDistanciaGuardadaActualmente.text = getString(R.string.texto_distancia_actual_guardada, distancia)
    }

    private fun actualizarTextoTarifaGuardada(tarifa: Float) {
        binding.textoTarifaGuardada.text = getString(R.string.texto_tarifa_actual, tarifa.toInt())
    }

    private fun actualizarTextoDistanciaViajeABGuardada(distancia: Float) {
        binding.textoDistanciaViajeAbGuardada.text = getString(R.string.texto_distancia_viaje_ab_guardada, distancia)
    }

    private fun actualizarTextoGananciaMinima(ganancia: Float) {
        binding.textoGananciaMinimaGuardada.text = getString(R.string.texto_ganancia_minima_guardada, ganancia.toInt())
    }

    private fun actualizarTextoFiltroPasajeroGuardado(isActivado: Boolean, calificacion: Float, viajes: Int) {
        binding.textoFiltroPasajeroGuardado.text = if (isActivado) {
            "Activado: Calificación >= $calificacion, Viajes >= $viajes"
        } else {
            "Filtro desactivado"
        }
    }

    private fun actualizarEstadoUiFiltroPasajero(isActivado: Boolean) {
        binding.layoutCalificacionMinima.isEnabled = isActivado
        binding.layoutViajesMinimos.isEnabled = isActivado
        binding.botonGuardarFiltroPasajero.isEnabled = isActivado
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevenir fugas de memoria
    }
}
