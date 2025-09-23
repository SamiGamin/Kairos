package com.kairos.ast.servicios.api

import android.util.Log
import com.kairos.ast.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Servicio para obtener rutas e información de geocodificación desde las APIs de Google.
 */
object DirectionsService {

    private const val API_KEY_GOOGLE = BuildConfig.GOOGLE_MAPS_API_KEY

    private val retrofitGoogle = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val googleGeocodingApi = retrofitGoogle.create(GoogleGeocodingApi::class.java)
    private val googleDirectionsApi = retrofitGoogle.create(GoogleDirectionsApi::class.java)

    /**
     * Convierte una dirección de texto en coordenadas "lat,lng" usando la API de Geocodificación de Google.
     */
    private suspend fun geocodificarDireccion(direccion: String): String? {
        try {
            val direccionLimpia = limpiarDireccion(direccion) // Usamos la función del archivo de utilidades
            Log.d("DirectionsService", "[Google Geocoding] Buscando coordenadas para: '$direccionLimpia'")
            val response = googleGeocodingApi.geocodeAddress(address = direccionLimpia, apiKey = API_KEY_GOOGLE)

            if (response.status == "OK" && response.results.isNotEmpty()) {
                val location = response.results[0].geometry.location
                Log.d("DirectionsService", "[Google Geocoding] Coordenadas encontradas: ${location.lat},${location.lng}")
                return "${location.lat},${location.lng}"
            } else {
                Log.w("DirectionsService", "[Google Geocoding] No se encontraron coordenadas para: '$direccionLimpia'. Status: ${response.status}")
                return null
            }
        } catch (e: Exception) {
            Log.e("DirectionsService", "[Google Geocoding] Fallo: ${e.message}", e)
            return null
        }
    }

    /**
     * Obtiene la información de distancia (en km) y duración (en minutos) de un viaje entre un origen y un destino.
     * Utiliza geocodificación para convertir direcciones a coordenadas si es necesario.
     */
    suspend fun obtenerInfoViajePrincipal(origen: String, destino: String): Pair<Float, Int>? {
        return withContext(Dispatchers.IO) {
            // Geocodificar origen y destino en paralelo
            val origenCoordsDeferred = async { geocodificarDireccion(origen) }
            val destinoCoordsDeferred = async { geocodificarDireccion(destino) }

            val origenCoords = origenCoordsDeferred.await()
            val destinoCoords = destinoCoordsDeferred.await()

            if (origenCoords == null || destinoCoords == null) {
                Log.e("DirectionsService", "[Google Routing] No se pudieron obtener las coordenadas para origen y/o destino.")
                return@withContext null
            }

            try {
                Log.d("DirectionsService", "[Google Routing] Solicitando ruta con coordenadas: Origen=$origenCoords | Destino=$destinoCoords")
                val response = googleDirectionsApi.getDirections(origenCoords, destinoCoords, API_KEY_GOOGLE)

                if (response.status == "OK" && response.routes.isNotEmpty() && response.routes[0].legs.isNotEmpty()) {
                    val leg = response.routes[0].legs[0]
                    val distanciaKm = (leg.distance.value / 1000.0).toFloat() // Distancia en metros a km
                    val duracionMin = (leg.duration.value / 60) // Duración en segundos a minutos

                    Log.i("DirectionsService", "[Google Routing] ¡ÉXITO! Distancia=${distanciaKm}km, Duración=${duracionMin}min")
                    return@withContext Pair(distanciaKm, duracionMin)
                } else {
                    Log.w("DirectionsService", "[Google Routing] No se encontró ruta. Status: ${response.status}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("DirectionsService", "[Google Routing] Fallo: ${e.message}", e)
                return@withContext null
            }
        }
    }
}