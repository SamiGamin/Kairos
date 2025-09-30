package com.kairos.ast.ui.perfil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentPerfilBinding
import com.kairos.ast.servicios.utils.DateUtils
import com.kairos.ast.ui.splash.SplashActivity

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.onEvent(PerfilEvent.OnAvatarSelected(it))
            }
        }
    }

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
            binding.progressBar.visibility = if (state.isLoading || state.isUploading) View.VISIBLE else View.GONE

            state.usuario?.let { usuario ->
                binding.tvUserName.text = usuario.nombre ?: "Sin nombre"
                binding.tvUserEmail.text = usuario.email

                // Controlar visibilidad del hint para añadir foto
                binding.hintAddPhoto.visibility = if (usuario.avatar_url.isNullOrEmpty()) View.VISIBLE else View.GONE

                // Cargar imagen de perfil con Glide
                Glide.with(this)
                    .load(usuario.avatar_url)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .circleCrop()
                    .into(binding.ivAvatar)

                // Cargar la información del plan (simplificado, ya que ahora viene del ViewModel)
                state.plan?.let {
                    binding.tvPlanStatus.text = "Plan: ${it.name}"
                } ?: run {
                    binding.tvPlanStatus.text = "Plan: ${usuario.tipo_plan.replaceFirstChar { it.titlecase() }}"
                }
                
                binding.estadoPlan.text = usuario.estado_plan
                val remainingDays = DateUtils.calculateRemainingDays(usuario.fecha_expiracion_plan)
                binding.tvPlanExpiry.text = "Vence en: $remainingDays días"
                val formattedDate = DateUtils.formatReadableDateTime(usuario.fecha_expiracion_plan)
                binding.tvfechaExpiracion.text = "Finaliza el: $formattedDate"
            }

            state.error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivAvatar.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

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