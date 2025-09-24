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
import com.kairos.ast.model.DeviceIdManager
import com.kairos.ast.model.PlanManager
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.servicios.Versiones.UpdateManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY = 1500L
        private const val TIMEOUT_DELAY = 10000L
        private const val TAG = "SplashActivity"
    }

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private var verificationCompleted = false

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

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
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@SplashActivity)
            updateManager.checkForUpdates {
                // Solo se ejecuta si no hay actualización
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }

        mostrarSplashInicial()

        handler.postDelayed({
            verificarEstadoUsuario()
        }, SPLASH_DELAY)

        // Timeout de seguridad
        handler.postDelayed({
            if (!verificationCompleted) {
                Log.w(TAG, "Timeout de verificación. Redirigiendo a LOGIN.")
                irA("LOGIN")
            }
        }, TIMEOUT_DELAY)
    }

    private fun mostrarSplashInicial() {
       binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"

    }

    private fun verificarEstadoUsuario() {
        coroutineScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()

                if (currentUser == null) {
                    Log.d(TAG, "No hay usuario logueado. Navegando a LOGIN.")
                    irA("LOGIN")
                    return@launch
                }

                Log.d(TAG, "Usuario logueado: ID=${currentUser.id}, Email=${currentUser.email}")

                val planStatus = PlanManager.verificarEstadoPlan(currentUser.id)
                val deviceValid = verificarDispositivo(currentUser.id)

                if (!deviceValid) {
                    mostrarErrorDispositivo()
                    return@launch
                }

                when (planStatus) {
                    PlanManager.PlanStatus.FREE_TRIAL,
                    PlanManager.PlanStatus.PAID -> {
                        irA("MAIN") // Ir a la app normal si todo está bien
                    }
                    PlanManager.PlanStatus.EXPIRED -> {
                        mostrarPlanExpirado(currentUser.id)
                    }
                    PlanManager.PlanStatus.ERROR -> {
                        mostrarAppNormalConAdvertencia()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al verificar el estado del usuario", e)
                mostrarAppNormalConAdvertencia()
            } finally {
                verificationCompleted = true
            }
        }
    }

    private suspend fun verificarDispositivo(userId: String): Boolean {
        return try {
            val deviceIdHash = DeviceIdManager.getSecureDeviceId(this)
            val result = SupabaseClient.client
                .from("dispositivos")
                .select {
                    filter {
                        eq("device_id_hash", deviceIdHash)
                        eq("usuario_id", userId)
                    }
                    limit(1)
                }
            !result.data.isNullOrBlank() && result.data != "[]"
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar el dispositivo.", e)
            true // Fail-open en caso de error durante el splash
        }
    }

    private fun mostrarAppNormalConAdvertencia() {
        Toast.makeText(
            this,
            "Modo offline - Verificación de plan no disponible",
            Toast.LENGTH_LONG
        ).show()
        irA("MAIN")
    }

    private fun mostrarPlanExpirado(userId: String) {
        Toast.makeText(
            this,
            "Tu plan ha expirado - Por favor renueva",
            Toast.LENGTH_LONG
        ).show()
        irA("MAIN")
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
                finish()
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
        handler.removeCallbacksAndMessages(null) // Limpiar el handler
    }
}