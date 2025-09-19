package com.kairos.ast

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.kairos.ast.databinding.ActivityMainBinding
import com.kairos.ast.ui.indrive.FragmentoIndrive // Importamos nuestro nuevo fragmento

class MainActivity : AppCompatActivity() {

    // Las constantes para SharedPreferences se mantienen aquí, ya que FragmentoIndrive las usa.
    companion object {
        const val PREFERENCIAS_APP_KAIROS = "PreferenciasAppKairos"
        const val CLAVE_DISTANCIA_MAXIMA_KM_LISTA = "distanciaMaximaKmLista"
        const val VALOR_POR_DEFECTO_DISTANCIA_KM = 5.0f
        const val CLAVE_TARIFA_POR_KM = "tarifaPorKm"
        const val VALOR_POR_DEFECTO_TARIFA_KM = 2500f // 2500 pesos colombianos por km
        
        // Nuevas constantes para la distancia máxima del viaje A-B
        const val CLAVE_DISTANCIA_MAXIMA_VIAJE_AB_KM = "distanciaMaximaViajeABKm"
        const val VALOR_POR_DEFECTO_DISTANCIA_VIAJE_AB_KM = 15.0f // Ejemplo: 15km por defecto
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

        configurarNavegacionInferior()

        // Cargar el fragmento inicial si no hay un estado guardado
        // Corregido R.id.fragmentoIndrive a R.id.navegacion_indrive_config para que coincida con el menú
        if (savedInstanceState == null) {
            binding.vistaNavegacionInferior.selectedItemId = R.id.fragmentoIndrive
        }
    }

    private fun configurarNavegacionInferior() {
        binding.vistaNavegacionInferior.setOnItemSelectedListener { item ->
            var fragmentoSeleccionado: Fragment? = null
            when (item.itemId) {
                // Corregido R.id.fragmentoIndrive a R.id.navegacion_indrive_config para que coincida con el menú
                R.id.fragmentoIndrive -> {
                    fragmentoSeleccionado = FragmentoIndrive()
                }
                // Puedes añadir más casos aquí para otros fragmentos en el futuro
            }

            if (fragmentoSeleccionado != null) {
                cargarFragmento(fragmentoSeleccionado)
                return@setOnItemSelectedListener true
            }
            false
        }
    }

    private fun cargarFragmento(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor_fragmentos, fragmento)
            .commit()
    }
    
    // La lógica de cargarConfiguracionGuardada, configurarListeners, guardarConfiguracionDistancia,
    // guardarConfiguracionTarifa, actualizarTextoDistanciaGuardada, y actualizarTextoTarifaGuardada
    // ha sido movida a FragmentoIndrive.kt
}
