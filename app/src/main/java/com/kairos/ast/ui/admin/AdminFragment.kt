package com.kairos.ast.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kairos.ast.databinding.FragmentAdminBinding
import com.kairos.ast.model.Usuario
import java.time.Instant

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
        setupFragmentResultListener() // Escucha los resultados del diálogo
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

    /**
     * Configura el listener para recibir datos desde el ManagePlanDialogFragment.
     */
    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener(ManagePlanDialogFragment.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            val userId = bundle.getString("userId")
            val newPlan = bundle.getString("newPlan")
            val newExpirationMillis = bundle.getLong("newExpiration")

            if (userId != null && newPlan != null && newExpirationMillis > 0) {
                val newExpiration = Instant.ofEpochMilli(newExpirationMillis)
                viewModel.updateUserPlan(userId, newPlan, newExpiration)
            } else {
                Toast.makeText(requireContext(), "No se recibieron datos para actualizar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Muestra el diálogo de gestión de planes para un usuario específico.
     */
    private fun showManagePlanDialog(usuario: Usuario) {
        val dialog = ManagePlanDialogFragment.newInstance(usuario)
        dialog.show(childFragmentManager, ManagePlanDialogFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}