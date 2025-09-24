package com.kairos.ast.ui.splash

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.kairos.ast.BuildConfig
import com.kairos.ast.MainActivity
import com.kairos.ast.R
import com.kairos.ast.databinding.ActivitySplashBinding
import com.kairos.ast.model.PlanManager
import com.kairos.ast.model.UserSessionValidator
import com.kairos.ast.model.ValidationResult
import com.kairos.ast.servicios.Versiones.UpdateManager
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TIMEOUT_DELAY = 15000L
        private const val TAG = "SplashActivity"
    }

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private var flowCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"

        // --- INICIO DEL FLUJO SECUENCIAL ---
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@SplashActivity)
            updateManager.checkForUpdates {
                Log.d(TAG, "App actualizada. Procediendo a validar sesión.")
                // Llama al nuevo validador centralizado
                lifecycleScope.launch {
                    val result = UserSessionValidator.validate(this@SplashActivity)
                    handleValidationResult(result)
                }
            }
        }
        // --- FIN DEL FLUJO SECUENCIAL ---

        // Timeout de seguridad para todo el proceso de inicio
        handler.postDelayed({
            if (!flowCompleted) {
                Log.w(TAG, "Timeout de verificación general. Redirigiendo a LOGIN.")
                irA("LOGIN")
            }
        }, TIMEOUT_DELAY)
    }

    /**
     * Procesa el resultado del validador y navega a la pantalla correspondiente.
     */
    private fun handleValidationResult(result: ValidationResult) {
        flowCompleted = true // Marcar que el flujo terminó para el timeout
        when (result) {
            is ValidationResult.Valid -> {
                // La validación es exitosa, no importa si es admin o no para la navegación inicial.
                Log.d(TAG, "Validación exitosa (isAdmin: ${result.isAdmin}). Navegando a MAIN.")
                irA("MAIN")
            }
            is ValidationResult.NoUser -> {
                Log.d(TAG, "Validación indica que no hay usuario. Navegando a LOGIN.")
                irA("LOGIN")
            }
            is ValidationResult.DeviceNotValid -> {
                Log.w(TAG, "Validación indica dispositivo no válido.")
                mostrarErrorDispositivo()
            }
            is ValidationResult.PlanNotValid -> {
                val message = if (result.status == PlanManager.PlanStatus.EXPIRED) "Tu plan ha expirado." else "No se pudo verificar tu plan."
                Log.w(TAG, "Validación indica plan no válido: ${result.status}")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                irA("MAIN") // Aún se permite el acceso
            }
            is ValidationResult.Error -> {
                Log.e(TAG, "Error en la validación: ${result.message}")
                Toast.makeText(this, "Modo offline - Error de conexión.", Toast.LENGTH_LONG).show()
                irA("MAIN") // Fail-open
            }
        }
    }

    private fun irA(destino: String) {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("START_DESTINATION", destino)
        }
        startActivity(intent)
        finish()
    }

    private fun mostrarErrorDispositivo() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Dispositivo No Autorizado")
            .setMessage("Este dispositivo ya ha utilizado la prueba gratuita. Por favor contacta con soporte.")
            .setPositiveButton("Contactar Soporte") { _, _ ->
                contactarSoporte()
            }
            .setNegativeButton("Cerrar") { _, _ ->
                finish() // Cierra la app
            }
            .setCancelable(false)
            .show()
    }

    private fun contactarSoporte() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("soporte@kairos.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Problema de dispositivo - App Kairos")
        }
        startActivity(Intent.createChooser(intent, "Contactar soporte"))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}