package com.kairos.ast

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.kairos.ast.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFERENCIAS_APP_KAIROS = "PreferenciasAppKairos"
        const val CLAVE_DISTANCIA_MAXIMA_KM_LISTA = "distanciaMaximaKmLista"
        const val VALOR_POR_DEFECTO_DISTANCIA_KM = 5.0f
        const val CLAVE_TARIFA_POR_KM = "tarifaPorKm"
        const val VALOR_POR_DEFECTO_TARIFA_KM = 2500f
        const val CLAVE_DISTANCIA_MAXIMA_VIAJE_AB_KM = "distanciaMaximaViajeABKm"
        const val VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM = 15.0f
        const val CLAVE_GANANCIA_MINIMA_VIAJE = "gananciaMinimaViaje"
        const val VALOR_POR_DEFECTO_GANANCIA_MINIMA_VIAJE = 10000f
        const val ACCION_ACTUALIZAR_CONFIGURACION = "com.kairos.ast.ACCION_ACTUALIZAR_CONFIGURACION"

    }

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // Evitar padding inferior para BottomNav
            insets
        }
        configurarNavegacion()
    }

    private fun configurarNavegacion() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.contenedor_fragmentos) as NavHostFragment
        val navController = navHostFragment.navController
        NavigationUI.setupWithNavController(binding.vistaNavegacionInferior, navController)

        // Navegación condicional basada en el Intent de SplashActivity
        val startDestination = intent.getStringExtra("START_DESTINATION")
        if (startDestination == "LOGIN") {
            // Usamos NavOptions para limpiar el backstack y que el usuario no pueda volver
            // al fragmento principal (que no debería ver) con el botón de atrás.
            val navOptions = NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
            navController.navigate(R.id.loginFragment, null, navOptions)
        }
    }
}
