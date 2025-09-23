package com.kairos.ast.servicios.api

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
