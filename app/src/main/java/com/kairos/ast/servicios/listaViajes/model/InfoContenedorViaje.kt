package com.kairos.ast.servicios.listaViajes.model

/**
 * Almacena la información extraída de un contenedor de viaje en la lista.
 *
 * @param distanciaRecogidaKm La distancia para recoger al pasajero, en kilómetros.
 * @param direccionOrigen La dirección de origen del viaje.
 * @param direccionDestino La dirección de destino del viaje.
 * @param tiempoViajeABMinutos El tiempo estimado del viaje A-B mostrado en la UI, en minutos.
 * @param distanciaViajeABKm La distancia estimada del viaje A-B mostrada en la UI, en kilómetros.
 */
data class InfoContenedorViaje(
    val distanciaRecogidaKm: Float?,
    val direccionOrigen: String?,
    val direccionDestino: String?,
    val tiempoViajeABMinutos: Int? = null,
    val distanciaViajeABKm: Float? = null
)
