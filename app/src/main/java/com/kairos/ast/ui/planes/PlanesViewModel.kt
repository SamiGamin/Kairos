package com.kairos.ast.ui.planes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.BuildConfig
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import com.kairos.ast.ui.planes.model.Plan
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

// Eventos para comunicar acciones desde el ViewModel a la UI
sealed class PlanEvent {
    data class OpenWhatsApp(val url: String) : PlanEvent()
    data class ShowError(val message: String) : PlanEvent()
}

class PlanesViewModel : ViewModel() {

    // LiveData para los eventos (abrir WhatsApp, mostrar error)
    private val _event = MutableLiveData<PlanEvent>()
    val event: LiveData<PlanEvent> = _event

    // LiveData para la lista de planes
    private val _plans = MutableLiveData<List<Plan>>()
    val plans: LiveData<List<Plan>> = _plans

    private val whatsappNumber = BuildConfig.WHATSAPP_NUMBER

    init {
        // Lanzar la carga de planes en una corrutina
        viewModelScope.launch {
            loadPlans()
        }
    }

    /**
     * Carga los planes disponibles desde la tabla 'planes' en Supabase.
     */
    private suspend fun loadPlans() {
        try {
            val planList = SupabaseClient.client.from("planes")
                .select {
                    // La política RLS ya filtra por is_enabled = true
                    order("display_order", Order.ASCENDING)
                }
                .decodeAs<List<Plan>>() // Usamos decodeAs para obtener una lista
            _plans.postValue(planList)
        } catch (e: Exception) {
            Log.e("PlanesViewModel", "Error al cargar los planes desde Supabase", e)
            _event.postValue(PlanEvent.ShowError("No se pudieron cargar los planes."))
        }
    }

    /**
     * Inicia el proceso de selección de un plan.
     * Obtiene los datos del usuario y genera el evento para abrir WhatsApp.
     */
    fun onPlanSelected(planName: String) {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user == null) {
                    _event.postValue(PlanEvent.ShowError("No se pudo obtener tu usuario. Por favor, reinicia sesión."))
                    return@launch
                }

                // Buscar nombre del usuario en la tabla "usuarios"
                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select { filter { eq("id", user.id) } }
                    .decodeSingle<Usuario>()

                val message = """
                Hola 👋, quiero activar el *$planName*.
                Mi nombre es: ${usuario.nombre}
                Correo: ${user.email ?: "No disponible"}
                """.trimIndent()

                val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                val url = "https://wa.me/$whatsappNumber?text=$encodedMessage"

                _event.postValue(PlanEvent.OpenWhatsApp(url))

            } catch (e: Exception) {
                Log.e("PlanesViewModel", "Error al seleccionar plan", e)
                _event.postValue(PlanEvent.ShowError("Ocurrió un error inesperado."))
            }
        }
    }
}