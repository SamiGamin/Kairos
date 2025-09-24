package com.kairos.ast.ui.planes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kairos.ast.databinding.FragmentPlanesBinding

class PlanesFragment : Fragment() {

    private var _binding: FragmentPlanesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanesViewModel by viewModels()
    private lateinit var planesAdapter: PlanesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        // El listener del adaptador ahora llama al método del ViewModel
        planesAdapter = PlanesAdapter { selectedPlan ->
            viewModel.onPlanSelected(selectedPlan.name)
        }
        binding.rvPlanes.apply {
            adapter = planesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        // Observador para la lista de planes
        viewModel.plans.observe(viewLifecycleOwner) { plans ->
            planesAdapter.submitList(plans)
        }

        // Observador para los eventos (abrir WhatsApp, mostrar errores)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PlanEvent.OpenWhatsApp -> {
                    openWhatsApp(event.url)
                    showConfirmationToast()
                }
                is PlanEvent.ShowError -> {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openWhatsApp(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp") // Intenta abrir WhatsApp directamente
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback si WhatsApp no está instalado
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir WhatsApp ni un navegador.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showConfirmationToast() {
        Toast.makeText(requireContext(), "Serás redirigido a WhatsApp para completar tu solicitud.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}