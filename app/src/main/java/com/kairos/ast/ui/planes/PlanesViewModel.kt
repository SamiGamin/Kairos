package com.kairos.ast.ui.planes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.ast.BuildConfig
import com.kairos.ast.model.SupabaseClient
import com.kairos.ast.model.Usuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

// Eventos para comunicar acciones desde el ViewModel a la UI
sealed class PlanEvent {
    data class OpenWhatsApp(val url: String) : PlanEvent()
    data class ShowError(val message: String) : PlanEvent()
}

class PlanesViewModel : ViewModel() {

    private val _event = MutableLiveData<PlanEvent>()
    val event: LiveData<PlanEvent> = _event

    // IMPORTANTE: Reemplaza este número por tu número de WhatsApp con el código de país

    private val whatsappNumber = BuildConfig.WHATSAPP_NUMBER


    /**
     * Inicia el proceso de selección de un plan.
     * Obtiene el ID del usuario y genera el evento para abrir WhatsApp.
     */
    fun onPlanSelected(planName: String) {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val userId = user?.id
                val userEmail = user?.email

                if (userId == null) {
                    _event.postValue(
                        PlanEvent.ShowError("No se pudo obtener tu usuario. Por favor, reinicia sesión.")
                    )
                    return@launch
                }

                // Buscar nombre del usuario en tu tabla "usuarios"
                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter { eq("id", userId) }
                        limit(1)
                    }
                    .decodeSingle<Usuario>()

                val nombre = usuario.nombre  // 👈 Asumiendo que tu data class Usuario tiene "nombre"

                val message = """
                Hola 👋, quiero activar el *$planName*.
                Mi nombre es: $nombre
                Correo: ${userEmail ?: "No disponible"}
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