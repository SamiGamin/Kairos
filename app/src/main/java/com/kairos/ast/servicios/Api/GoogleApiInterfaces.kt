package com.kairos.ast.servicios.api

import retrofit2.http.GET
import retrofit2.http.Query

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
