package com.kairos.ast.ui.reguistro

// CAMBIO V3: Imports actualizados desde 'gotrue' a 'auth'

// CAMBIO V3: Mantenemos 'from', pero eliminamos el import de 'insert'
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kairos.ast.R
import com.kairos.ast.databinding.FragmentRegistroBinding
import com.kairos.ast.model.DeviceIdManager
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient

class RegistroFragment : Fragment() {
    private var _binding: FragmentRegistroBinding? = null
    private val binding get() = _binding!!

    // CAMBIO V3: Referencia al objeto correcto
    private val supabase: JanSupabaseClient by lazy {
        SupabaseClient.client
    }

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
    }

    private fun configurarListeners() {
        binding.btnRegistrarse.setOnClickListener {
            realizarRegistro()
        }
    }

    private fun realizarRegistro() {
        val nombre = binding.etNombre.text.toString().trim()
        val emailInput = binding.etEmail.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val passwordInput = binding.etPassword.text.toString()

        if (!validarFormulario(nombre, emailInput, passwordInput)) {
            return
        }

        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                Log.d("RegistroFragment", "Iniciando registro...")

                val deviceIdHash = DeviceIdManager.getSecureDeviceId(requireContext())
                Log.d("RegistroFragment", "DeviceId obtenido: $deviceIdHash")
                if (DeviceIdManager.hasDeviceUsedFreeTrial(deviceIdHash)) {
                    Log.e("RegistroFragment", "El dispositivo ya usó la prueba gratuita")
                    throw Exception("Este dispositivo ya ha utilizado el período de prueba gratuito.")
                }

                // 2. Registrar usuario en Supabase Auth
                Log.d("RegistroFragment", "Registrando usuario en Supabase con email: $emailInput")
                supabase.auth.signUpWith(Email) {
                    email = emailInput
                    password = passwordInput
                }
                val user = supabase.auth.currentUserOrNull()
                if (user == null) {
                    Log.e("RegistroFragment", "El usuario es null después del signUp")
                    throw Exception("No se pudo obtener el usuario recién creado")
                }
                Log.d("RegistroFragment", "Usuario creado con ID: ${user.id} y email: ${user.email}")


                // 3. Crear el objeto Usuario
                val nuevoUsuario = Usuario(
                    id = user.id,
                    email = emailInput,
                    nombre = nombre,
                    telefono = if (telefono.isNotEmpty()) telefono else null
                )

                Log.d("RegistroFragment", "Insertando usuario en tabla 'usuarios': $nuevoUsuario")
                supabase.from("usuarios").insert(nuevoUsuario)

                // 5. Registrar el dispositivo
                DeviceIdManager.registrarDispositivo(deviceIdHash, user.id)
                Log.d("RegistroFragment", "Dispositivo registrado con usuario: ${user.id}")

                // 6. Registro exitoso
                mostrarMensajeExitoso("¡Cuenta creada exitosamente! Sesión iniciada.")
                Log.d("RegistroFragment", "Registro finalizado correctamente")
                limpiarCampos()

                // 7. Navegar al fragmento de permisos
                findNavController().navigate(R.id.action_registroFragment_to_permisosFragment)

            } catch (e: Exception) {
                Log.e("RegistroFragment", "Error en registro", e)
                manejarErrorRegistro(e)
            } finally {
                mostrarCargando(false)
            }
        }
    }

    // (El resto de las funciones de validación y UI se mantienen igual)

    private fun validarFormulario(nombre: String, email: String, password: String): Boolean {
        if (nombre.isEmpty()) {
            binding.etNombre.error = "El nombre es requerido"
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Ingresa un email válido"
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        return true
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnRegistrarse.isEnabled = !mostrar
        binding.btnRegistrarse.text = if (mostrar) "Creando cuenta..." else "Crear Cuenta"
    }

    private fun mostrarMensajeExitoso(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }

    private fun manejarErrorRegistro(error: Exception) {
        // Mapeo de errores comunes en V3
        val mensajeError = when {
            error.message?.contains("Este dispositivo ya ha utilizado el período de prueba") == true ->
                error.message
            // Ajuste de mensajes de error comunes de Auth V3
            error.message?.contains("User already registered", ignoreCase = true) == true ->
                "Este email ya está registrado."
            error.message?.contains("Password should be at least", ignoreCase = true) == true ->
                "La contraseña no cumple los requisitos mínimos."
            error.message?.contains("network", ignoreCase = true) == true ->
                "Error de conexión. Verifica tu internet."
            else -> "Error al crear la cuenta: ${error.message}"
        }

        Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_LONG).show()
    }

    private fun limpiarCampos() {
        binding.etNombre.text?.clear()
        binding.etEmail.text?.clear()
        binding.etTelefono.text?.clear()
        binding.etPassword.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}