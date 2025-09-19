package com.kairos.ast.servicios.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Modelos de datos para la API de Google ---

// Para la API de Geocodificación de Google
data class GoogleGeocodingResponse(val results: List<GeocodingResult>, val status: String)
data class GeocodingResult(val geometry: Geometry)
data class Geometry(val location: Location)
data class Location(val lat: Double, val lng: Double)

// Para la API de Direcciones de Google
data class GoogleDirectionsResponse(val routes: List<Route>, val status: String)
data class Route(val legs: List<Leg>)
data class Leg(val distance: Distance, val duration: Duration)
data class Distance(val value: Int) // en metros
data class Duration(val value: Int) // en segundos

// --- INTERFACES DE RETROFIT PARA GOOGLE ---
interface GoogleGeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun geocodeAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GoogleGeocodingResponse
}

interface GoogleDirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,      // "lat,lng" o dirección
        @Query("destination") destination: String, // "lat,lng" o dirección
        @Query("key") apiKey: String
    ): GoogleDirectionsResponse
}

/**
 * Servicio para obtener rutas e información de geocodificación desde las APIs de Google.
 */
object DirectionsService {

    // IMPORTANTE: Esta es la API Key que proporcionaste.
    private const val API_KEY_GOOGLE = "AIzaSyBK-N8nUWnaSRZ7fVmSOSQspiQeAau7NyY"

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
            val direccionLimpia = limpiarDireccion(direccion) // Podemos reusar la limpieza si aplica
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

    /**
     * Limpia y estandariza una dirección. Esta función podría necesitar ajustes
     * dependiendo de qué tan bien la API de Google maneje las direcciones originales.
     */
    private fun limpiarDireccion(direccion: String): String {
        return direccion
            // 1. Quitar texto entre paréntesis
            .replace(Regex("""\s*\(.*?\)"""), "")
            // 2. Quitar sufijos de empresa
            .replace(Regex("""\s*S\.A\.S\.?|\s*S\.A\.?|\s*Ltda\.?""", RegexOption.IGNORE_CASE), "")
            // 3. Reemplazar abreviaturas comunes con punto o sin punto
            .replace("Cl\\.?", "Calle", ignoreCase = true)
            .replace("Cra\\.?", "Carrera", ignoreCase = true)
            .replace("Tv\\.?", "Transversal", ignoreCase = true)
            .replace("Dg\\.?", "Diagonal", ignoreCase = true)
            // 4. Eliminar los caracteres '#' y '-' que confunden al geocodificador.
            .replace("#", " ")
            .replace("-", " ")
            // 5. Dejar solo letras, números y espacios para eliminar cualquier otro símbolo.
            .replace(Regex("""[^a-zA-Z0-9\s]"""), "")
            // 6. Limpiar espacios múltiples que puedan haber quedado
            .replace(Regex("""\s+"""), " ")
            .trim()
            // 7. Asegurarse de que termine con "Bogota, Colombia" si no lo tiene.
            // Esto podría ser específico de GraphHopper y quizás no tan necesario para Google.
            // Considera si quieres mantenerlo o ajustarlo.
            .let { if (!it.contains("Bogota", true) && !it.contains("Colombia", true)) "$it, Bogota, Colombia" else it }
    }
}
