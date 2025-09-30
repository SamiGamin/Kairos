package com.kairos.ast.ui.perfil

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kairos.ast.model.Plan
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

// State for the UI
data class PerfilUiState(
    val isLoading: Boolean = true,
    val usuario: Usuario? = null,
    val plan: Plan? = null,
    val error: String? = null
)

// Events from the UI
sealed class PerfilEvent {
    object OnLogout : PerfilEvent()
    object OnChangePassword : PerfilEvent()
    object OnManageSubscription : PerfilEvent()
}

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PerfilViewModel"
    }

    private val _uiState = MutableLiveData<PerfilUiState>()
    val uiState: LiveData<PerfilUiState> = _uiState

    private var planes: List<Plan> = emptyList()

    init {
        loadPlans()
        loadUserProfile()
    }

    fun onEvent(event: PerfilEvent) {
        when (event) {
            is PerfilEvent.OnLogout -> logout()
            // Handle other events later
            else -> {}
        }
    }

    private fun loadPlans() {
        try {
            val jsonString = getApplication<Application>().assets.open("planes_rows.json").bufferedReader().use { it.readText() }
            planes = Json.decodeFromString<List<Plan>>(jsonString)
            Log.d(TAG, "Planes cargados exitosamente: ${planes.size} planes encontrados.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar o parsear los planes desde assets", e)
            _uiState.postValue(PerfilUiState(isLoading = false, error = "Error al cargar la configuración de planes."))
        }
    }

    private fun loadUserProfile() {
        _uiState.value = PerfilUiState(isLoading = true)
        Log.d(TAG, "Iniciando carga de perfil de usuario...")
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user == null) {
                    Log.e(TAG, "Error: No se encontró sesión de usuario activa.")
                    _uiState.postValue(PerfilUiState(isLoading = false, error = "No se pudo encontrar la sesión del usuario."))
                    return@launch
                }
                Log.d(TAG, "Usuario autenticado encontrado: ${user.id}")

                // Fetch profile from 'usuarios' table
                Log.d(TAG, "Consultando tabla 'usuarios' para el id: ${user.id}")
                val result = SupabaseClient.client.from("usuarios").select {
                    filter { eq("id", user.id) }
                }
                Log.d(TAG, "Respuesta de Supabase: ${result.data}")
                val perfilUsuario = Json.decodeFromString<List<Usuario>>(result.data).first()
                Log.d(TAG, "Perfil decodificado: $perfilUsuario")

                // Find the user's plan from the loaded plans
                val planUsuario = planes.find { it.id == perfilUsuario.tipo_plan }
                if (planUsuario == null) {
                    Log.w(TAG, "No se encontró un plan que coincida con el tipo de plan del usuario: ${perfilUsuario.tipo_plan}")
                }

                _uiState.postValue(PerfilUiState(isLoading = false, usuario = perfilUsuario, plan = planUsuario))

            } catch (e: Exception) {
                Log.e(TAG, "Excepción al cargar el perfil de usuario", e)
                _uiState.postValue(PerfilUiState(isLoading = false, error = "Error al cargar el perfil: ${e.message}"))
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión", e)
            }
        }
    }
}