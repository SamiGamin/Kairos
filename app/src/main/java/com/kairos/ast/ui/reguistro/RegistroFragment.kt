package com.kairos.ast.ui.reguistro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentRegistroBinding

class RegistroFragment : Fragment() {
    private var _binding: FragmentRegistroBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarListeners()
        observarViewModel()
    }

    private fun configurarListeners() {
        binding.btnRegistrarse.setOnClickListener {
            intentarRegistro()
        }
        binding.enlaceLogin.setOnClickListener {
            irAlLogin()
        }
    }

    private fun irAlLogin() {
        findNavController().navigate(R.id.action_registroFragment_to_loginFragment)
    }

    private fun observarViewModel() {
        viewModel.registroState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegistroState.Loading -> mostrarCargando(true)
                is RegistroState.Success -> {
                    mostrarCargando(false)
                    Toast.makeText(requireContext(), "¡Cuenta creada exitosamente! Sesión iniciada.", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_registroFragment_to_permisosFragment)
                }
                is RegistroState.Error -> {
                    mostrarCargando(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun intentarRegistro() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (validarFormulario(nombre, email, password)) {
            viewModel.realizarRegistro(requireContext(), nombre, email, telefono, password)
        }
    }

    private fun validarFormulario(nombre: String, email: String, password: String): Boolean {
        // La validación de formulario puede permanecer aquí, ya que está directamente ligada a la UI.
        var esValido = true
        if (nombre.isEmpty()) {
            binding.tilNombre.error = "El nombre es requerido"
            esValido = false
        } else {
            binding.tilNombre.error = null
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Ingresa un email válido"
            esValido = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.length < 6) {
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            esValido = false
        } else {
            binding.tilPassword.error = null
        }
        return esValido
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnRegistrarse.isEnabled = !mostrar
        binding.btnRegistrarse.text = if (mostrar) "Creando cuenta..." else "Crear Cuenta"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}