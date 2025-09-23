package com.kairos.ast.ui.planes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kairos.ast.databinding.FragmentPlanesBinding

class PlanesFragment : Fragment() {

    private var _binding: FragmentPlanesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSeleccionarMensual.setOnClickListener {
            // Lógica para seleccionar el plan mensual
            Toast.makeText(requireContext(), "Has seleccionado el Plan Mensual", Toast.LENGTH_SHORT).show()
        }

        binding.btnSeleccionarAnual.setOnClickListener {
            // Lógica para seleccionar el plan anual
            Toast.makeText(requireContext(), "Has seleccionado el Plan Anual", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}