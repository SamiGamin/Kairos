package com.kairos.ast.ui.Login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.forgotPasswordState.observe(viewLifecycleOwner) { state ->
            showLoading(false)
            when (state) {
                is ForgotPasswordState.Loading -> showLoading(true)
                is ForgotPasswordState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack() // Volver a la pantalla anterior (Login)
                }
                is ForgotPasswordState.Error -> {
                    binding.tilEmail.error = state.message
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            binding.tilEmail.error = null // Limpiar error anterior
            val email = binding.etEmail.text.toString().trim()
            viewModel.sendPasswordReset(email)
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSend.isEnabled = !loading
        binding.btnSend.text = if (loading) "Enviando..." else "Enviar Correo"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}