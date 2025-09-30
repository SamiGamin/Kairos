package com.kairos.ast

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.kairos.ast.databinding.ActivityMainBinding
import com.kairos.ast.model.UserRoleManager
import com.kairos.ast.ui.dialogs.PlanExpiradoDialogFragment

class MainActivity : AppCompatActivity(), PlanExpiradoDialogFragment.PlanExpiradoDialogListener {

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

        // Claves para filtros opcionales
        const val CLAVE_FILTRAR_CALIFICACION = "filtrarPorCalificacion"
        const val VALOR_POR_DEFECTO_FILTRAR_CALIFICACION = false
        const val CLAVE_MIN_CALIFICACION = "minCalificacion"
        const val VALOR_POR_DEFECTO_MIN_CALIFICACION = 4.5f
        const val CLAVE_FILTRAR_NUMERO_VIAJES = "filtrarPorNumeroDeViajes"
        const val VALOR_POR_DEFECTO_FILTRAR_NUMERO_VIAJES = false
        const val CLAVE_MIN_VIAJES = "minViajes"
        const val VALOR_POR_DEFECTO_MIN_VIAJES = 100

        // Clave para acciones automáticas
        const val CLAVE_ACCIONES_AUTOMATICAS = "accionesAutomaticas"
        const val VALOR_POR_DEFECTO_ACCIONES_AUTOMATICAS = true

        const val ACCION_ACTUALIZAR_CONFIGURACION = "com.kairos.ast.ACCION_ACTUALIZAR_CONFIGURACION"

    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController


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
        setupPlanObservers()
        setupAdminMenu() // Comprueba y configura el menú de admin
    }

    private fun configurarNavegacion() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.contenedor_fragmentos) as NavHostFragment
        navController = navHostFragment.navController

        // Definir los destinos de nivel superior (los que están en el BottomNavigationView)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragmentoIndrive, R.id.permisosFragment, R.id.perfilFragment, R.id.adminFragment, R.id.planesFragment
            )
        )

        // Configurar el BottomNavigationView con el NavController y la configuración de AppBar
        NavigationUI.setupWithNavController(binding.vistaNavegacionInferior, navController)

        // Navegación condicional basada en el Intent de SplashActivity
        val startDestination = intent.getStringExtra("START_DESTINATION")
        if (startDestination == "LOGIN") {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
            navController.navigate(R.id.loginFragment, null, navOptions)
        }
    }

    private fun setupPlanObservers() {
        viewModel.usuario.observe(this) { usuario ->
            val menu = binding.vistaNavegacionInferior.menu
            val isPlanActive = usuario != null && usuario.estado_plan == "activo"

            // Ocultar funciones si el plan no está activo para una UI más limpia
            menu.findItem(R.id.fragmentoIndrive)?.isVisible = isPlanActive
            menu.findItem(R.id.permisosFragment)?.isVisible = isPlanActive

            // El perfil siempre está habilitado para poder gestionar la cuenta
            menu.findItem(R.id.perfilFragment)?.isEnabled = true
            menu.findItem(R.id.planesFragment)?.isEnabled = true

            // Mostrar diálogo de plan expirado si corresponde
            // NOTA: Asegúrate de que 'vencido' sea el valor correcto para el estado del plan expirado en tu base de datos.
            if (usuario?.estado_plan == "expirado" && !viewModel.planExpiradoDialogMostrado) {
                PlanExpiradoDialogFragment().show(supportFragmentManager, "PlanExpiradoDialog")
                viewModel.planExpiradoDialogMostrado = true
            }
        }
    }

    /**
     * Comprueba el rol del usuario y muestra u oculta el menú de administración.
     */
    private fun setupAdminMenu() {
        val menu = binding.vistaNavegacionInferior.menu
        val adminMenuItem = menu.findItem(R.id.adminFragment)
        adminMenuItem?.isVisible = UserRoleManager.isAdmin(this)
        val userPlanMenuItem = menu.findItem(R.id.planesFragment)
        userPlanMenuItem?.isVisible = !UserRoleManager.isAdmin(this)

    }

    override fun onVerPlanesClicked() {
        navController.navigate(R.id.planesFragment)
    }
}