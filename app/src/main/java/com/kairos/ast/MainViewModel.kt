package com.kairos.ast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainViewModel : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadUserProfile()
    }

    fun actualizarEstadoUsuario() {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user != null) {
                    val perfilUsuarioJson = SupabaseClient.client.from("usuarios").select {
                        filter { eq("id", user.id) }
                    }.data
                    val perfilUsuario = json.decodeFromString<List<Usuario>>(perfilUsuarioJson).firstOrNull()
                    _usuario.postValue(perfilUsuario)
                } else {
                    _usuario.postValue(null)
                }
            } catch (e: Exception) {
                _usuario.postValue(null) // Si hay error, se trata como si no tuviera plan activo
            }
        }
    }
}
