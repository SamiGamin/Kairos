package com.kairos.ast.ui.admin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc // Import para RPC
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant

sealed class AdminState {
    object Loading : AdminState()
    data class Success(val users: List<Usuario>) : AdminState()
    data class Error(val message: String) : AdminState()
}

class AdminViewModel : ViewModel() {

    private val _state = MutableLiveData<AdminState>()
    val state: LiveData<AdminState> = _state

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        _state.value = AdminState.Loading
        viewModelScope.launch {
            try {
                val users = SupabaseClient.client.postgrest.rpc(
                    "get_all_users",
                    Unit // 👈 si tu función no recibe parámetros
                ).decodeList<Usuario>()

                _state.postValue(AdminState.Success(users))
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al obtener usuarios", e)
                _state.postValue(AdminState.Error("Error al obtener usuarios: ${e.message}"))
            }
        }
    }



    fun updateUserPlan(userId: String, tipoPlan: String, fechaExpiracion: Instant) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.from("usuarios")
                    .update({
                        set("tipo_plan", tipoPlan)
                        set("estado_plan", "activo") // Se reactiva el plan al actualizar
                        set("fecha_expiracion_plan", fechaExpiracion)
                    }) {
                        filter {
                            eq("id", userId)
                        }
                    }
                _updateResult.postValue(Result.success(Unit))
                // Refrescar la lista de usuarios después de una actualización exitosa
                fetchUsers()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al actualizar el plan", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }
}