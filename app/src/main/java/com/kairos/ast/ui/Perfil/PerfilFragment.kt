package com.kairos.ast.ui.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentPerfilBinding
import com.kairos.ast.servicios.utils.DateUtils
import com.kairos.ast.ui.splash.SplashActivity

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }
    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.usuario?.let { usuario ->
                binding.tvUserName.text = usuario.nombre ?: "Sin nombre"
                binding.tvUserEmail.text = usuario.email

                // Cargar la información del plan
                binding.tvPlanStatus.text = "Plan: ${usuario.tipo_plan.replaceFirstChar { it.titlecase() }}"
                binding.estadoPlan.text = usuario.estado_plan

                if (usuario.tipo_plan == "gratuito") {
                    binding.tvPlanStatus.text = "Plan: ${usuario.tipo_plan.replaceFirstChar { it.titlecase() }}"
                    val remainingDays = DateUtils.calculateRemainingDays(usuario.fecha_expiracion_plan)
                    binding.tvPlanExpiry.text = "Vence en: $remainingDays días"
                    binding.tvPlanExpiry.visibility = View.VISIBLE

                    val formattedDate = DateUtils.formatReadableDateTime(usuario.fecha_expiracion_plan)
                    binding.tvfechaExpiracion.text = "Finaliza el: $formattedDate"
                } else {
                    binding.tvPlanExpiry.visibility = View.GONE
                    binding.tvfechaExpiracion.text = ""
                }
            }

            state.error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            viewModel.onEvent(PerfilEvent.OnLogout)
            // After logout, restart the app flow from Splash
            val intent = Intent(requireActivity(), SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }


        binding.btnManageSubscription.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_planesFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}