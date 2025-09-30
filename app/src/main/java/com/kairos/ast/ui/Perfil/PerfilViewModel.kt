package com.kairos.ast.ui.perfil

import android.app.Application
import android.net.Uri
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.UUID

// State for the UI
data class PerfilUiState(
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val usuario: Usuario? = null,
    val plan: Plan? = null,
    val error: String? = null
)

// Events from the UI
sealed class PerfilEvent {
    object OnLogout : PerfilEvent()
    data class OnAvatarSelected(val uri: Uri) : PerfilEvent()
}

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PerfilViewModel"
        private const val AVATAR_BUCKET = "avatars"
    }

    private val _uiState = MutableLiveData<PerfilUiState>()
    val uiState: LiveData<PerfilUiState> = _uiState

    private var planes: List<Plan> = emptyList()

    init {
        // loadPlans() // Se asume que los planes ahora vienen de Supabase
        loadUserProfile()
    }

    fun onEvent(event: PerfilEvent) {
        when (event) {
            is PerfilEvent.OnLogout -> logout()
            is PerfilEvent.OnAvatarSelected -> uploadAvatar(event.uri)
        }
    }

    private fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isUploading = true)
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull() ?: throw IllegalStateException("Usuario no autenticado")
                val oldAvatarUrl = _uiState.value?.usuario?.avatar_url

                val fileBytes = getApplication<Application>().contentResolver.openInputStream(uri)?.readBytes() ?: throw IllegalStateException("No se pudo leer el archivo de imagen")
                
                val filePath = "${user.id}/${UUID.randomUUID()}.jpg"

                // 1. Subir la nueva imagen
                val storage = SupabaseClient.client.storage
                storage.from(AVATAR_BUCKET).upload(filePath, fileBytes) {
                    upsert = true
                }

                // 2. Obtener la URL pública de la nueva imagen
                val publicUrl = storage.from(AVATAR_BUCKET).publicUrl(filePath)

                // 3. Actualizar la tabla de usuarios con la nueva URL
                val updates = mapOf("avatar_url" to publicUrl)
                SupabaseClient.client.from("usuarios").update(updates) {
                    filter { eq("id", user.id) }
                }

                // 4. Eliminar la imagen anterior si existía
                if (!oldAvatarUrl.isNullOrEmpty()) {
                    try {
                        val oldFilePath = oldAvatarUrl.substringAfter("$AVATAR_BUCKET/")
                        Log.d(TAG, "Eliminando avatar anterior: $oldFilePath")
                        storage.from(AVATAR_BUCKET).delete(listOf(oldFilePath))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar el avatar anterior (puede que ya no exista).", e)
                        // No detenemos el flujo si la eliminación falla.
                    }
                }

                // 5. Recargar el perfil para mostrar la nueva imagen
                loadUserProfile()

            } catch (e: Exception) {
                Log.e(TAG, "Error al subir el avatar", e)
                _uiState.postValue(_uiState.value?.copy(isUploading = false, error = "Error al subir la imagen: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value?.copy(isUploading = false)
            }
        }
    }

    private fun loadUserProfile() {
        _uiState.value = _uiState.value?.copy(isLoading = true) ?: PerfilUiState(isLoading = true)
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
                val result = SupabaseClient.client.from("usuarios").select {
                    filter { eq("id", user.id) }
                }
                val perfilUsuario = Json { ignoreUnknownKeys = true }.decodeFromString<List<Usuario>>(result.data).first()
                Log.d(TAG, "Perfil decodificado: $perfilUsuario")

                // Aquí iría la lógica para cargar los planes desde Supabase si fuera necesario
                // val planUsuario = planes.find { it.id == perfilUsuario.tipo_plan }

                _uiState.postValue(PerfilUiState(isLoading = false, usuario = perfilUsuario, plan = null))

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