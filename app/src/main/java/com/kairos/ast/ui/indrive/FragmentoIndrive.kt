package com.kairos.ast.ui.indrive

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    private fun configurarListeners() {
        binding.botonGuardarConfigDistancia.setOnClickListener {
            guardarConfiguracionDistanciaRecogida()
        }

        binding.botonGuardarTarifa.setOnClickListener {
            guardarConfiguracionTarifa()
        }

        binding.botonGuardarConfigDistanciaViajeAb.setOnClickListener {
            guardarConfiguracionDistanciaViajeAB()
        }
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
            actualizarTextoDistanciaViajeABGuardada(nuevaDistancia)
            Toast.makeText(requireContext(), getString(R.string.toast_distancia_viaje_ab_guardada, nuevaDistancia), Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.toast_error_formato_distancia_viaje_ab), Toast.LENGTH_LONG).show()
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevenir fugas de memoria
    }
}
