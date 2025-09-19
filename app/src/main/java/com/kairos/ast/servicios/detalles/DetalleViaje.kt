package com.kairos.ast.servicios.detalles

import android.view.accessibility.AccessibilityNodeInfo

// Archivo: DetalleViaje.kt

/**
 * Clase de datos (Modelo) que almacena toda la información extraída
 * de la pantalla de detalle de un viaje.
 */
data class DetalleViaje(
    val origen: String?,
    val destino: String?,
    val precioSugeridoNumerico: Float?,
    val distanciaViajeRealKm: Float?, // Distancia obtenida de la API de Directions
    val tiempoEstimadoMinutos: Int?,    // Tiempo estimado del viaje A-B mostrado en la UI
    val distanciaEstimadaKm: Float?,  // Distancia estimada del viaje A-B mostrada en la UI
    val botones: BotonesViaje
)

/**
 * Clase de datos anidada para almacenar la información de los botones.
 */
data class BotonesViaje(
    @Transient val nodoBotonAceptar: AccessibilityNodeInfo?,
    @Transient val nodoBotonEditar: AccessibilityNodeInfo?,
    val sugerencias: List<String>
)