package com.kairos.ast.ui.Login

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kairos.ast.MainActivity
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentLoginBinding
import com.kairos.ast.model.PlanManager
import com.kairos.ast.model.UserSessionValidator
import com.kairos.ast.model.ValidationResult
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            // Implementar si es necesario
        }

        binding.etEmail.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.tilEmail.error = null }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.tilPassword.error = null }
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
        showLoading(true, "Verificando plan...")

        // Después del login, validar la sesión para decidir a dónde ir.
        lifecycleScope.launch {
            val result = UserSessionValidator.validate(requireContext())
            handleValidationResult(result)
        }
    }

    private fun handleValidationResult(result: ValidationResult) {
        if (!isAdded) return // Asegurarse de que el fragmento todavía está adjunto
        showLoading(false)
        when (result) {
            is ValidationResult.Valid -> {
                Log.d("LoginFragment", "Validación exitosa. Navegando a MAIN.")
                irA("MAIN")
            }
            is ValidationResult.DeviceNotValid -> {
                Log.w("LoginFragment", "Validación indica dispositivo no válido.")
                mostrarErrorDispositivo()
            }
            is ValidationResult.PlanNotValid -> {
                val message = if (result.status == PlanManager.PlanStatus.EXPIRED) "Tu plan ha expirado." else "No se pudo verificar tu plan."
                Log.w("LoginFragment", "Validación indica plan no válido: ${result.status}")
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                irA("MAIN") // Aún se permite el acceso
            }
            is ValidationResult.Error -> {
                Log.e("LoginFragment", "Error en la validación: ${result.message}")
                Toast.makeText(requireContext(), "No se pudo verificar tu sesión. Entrando en modo offline.", Toast.LENGTH_LONG).show()
                irA("MAIN") // Fail-open
            }
            is ValidationResult.NoUser -> {
                Log.e("LoginFragment", "Error: No se encontró usuario después de un login exitoso.")
                showError("Error al verificar la sesión.")
            }
        }
    }

    private fun irA(destino: String) {
        val activity = requireActivity()
        if (activity.isFinishing || activity.isDestroyed) return

        val intent = Intent(activity, MainActivity::class.java).apply {
            putExtra("START_DESTINATION", destino)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity.finish()
    }

    private fun mostrarErrorDispositivo() {
        if (!isAdded || requireActivity().isFinishing) return
        AlertDialog.Builder(requireContext())
            .setTitle("Dispositivo No Autorizado")
            .setMessage("Este dispositivo ya ha utilizado la prueba gratuita. Por favor contacta con soporte.")
            .setPositiveButton("Contactar Soporte") { _, _ ->
                // Lógica para contactar soporte
            }
            .setNegativeButton("Cerrar") { _, _ ->
                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    private fun showLoading(loading: Boolean, text: String = "Iniciar Sesión") {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) text else "Iniciar Sesión"
    }

    private fun navigateToRegister() {
        findNavController().navigate(R.id.action_loginFragment_to_registroFragment)
    }

    private fun navigateToForgotPassword() {
        findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}