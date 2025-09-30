package com.kairos.ast.servicios.listaViajes.model

import com.kairos.ast.servicios.listaViajes.utils.DatosCalificacion


data class InfoContenedorViaje(
    val distanciaRecogidaKm: Float?,
    val direccionOrigen: String?,
    val direccionDestino: String?,
    val tiempoViajeAB: Int?,
    val distanciaViajeAB: Float?,
    val datosCalificacion: DatosCalificacion?
)
