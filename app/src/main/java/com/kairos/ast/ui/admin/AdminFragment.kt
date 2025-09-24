package com.kairos.ast.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.kairos.ast.databinding.FragmentAdminBinding
import com.kairos.ast.model.Usuario
import java.time.Instant
import java.time.ZoneOffset

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var userAdapter: UserAdminAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdminAdapter { usuario ->
            showManagePlanDialog(usuario)
        }
        binding.rvUsuarios.adapter = userAdapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchUsers()
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshLayout.isRefreshing = state is AdminState.Loading
            when (state) {
                is AdminState.Success -> userAdapter.submitList(state.users)
                is AdminState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                is AdminState.Loading -> { /* Handled by isRefreshing */ }
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Plan actualizado con éxito", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), "Error al actualizar: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showManagePlanDialog(usuario: Usuario) {
        // Inflar el layout del diálogo (necesitamos crearlo)
        // Por ahora, usaremos un diálogo simple de Material
        val planOptions = arrayOf("Gratuito", "Mensual", "Anual")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, planOptions)

        // Lógica para mostrar un diálogo y actualizar
        // Esto es un placeholder, idealmente se haría con un layout custom
        val selectedPlan = "Mensual" // Placeholder

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de expiración")
            .setSelection(Instant.now().toEpochMilli())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val newExpiration = Instant.ofEpochMilli(selection)
            viewModel.updateUserPlan(usuario.id, selectedPlan.lowercase(), newExpiration)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}