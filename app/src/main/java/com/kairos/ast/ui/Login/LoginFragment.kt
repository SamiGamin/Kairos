package com.kairos.ast.ui.Login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }
    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> showLoading(true)
                is LoginState.Success -> onLoginSuccess()
                is LoginState.Error -> showError(state.message)
            }
        }

        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            errors.emailError?.let { binding.tilEmail.error = it }
            errors.passwordError?.let { binding.tilPassword.error = it }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvRegister.setOnClickListener {
            navigateToRegister()
        }

        binding.tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        binding.tvDemoMode.setOnClickListener {
            startDemoMode()
        }

        // Limpiar errores cuando el usuario empiece a escribir
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilEmail.error = null
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (viewModel.validateForm(email, password)) {
            viewModel.login(email, password)
        }
    }

    private fun onLoginSuccess() {
        Toast.makeText(requireContext(), "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()

        // Navegar a la pantalla principal
        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Iniciando sesión..." else "Iniciar Sesión"
    }

    private fun navigateToRegister() {
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    private fun navigateToForgotPassword() {
        findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
    }

    private fun startDemoMode() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}