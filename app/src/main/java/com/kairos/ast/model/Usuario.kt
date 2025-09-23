package com.kairos.ast.model

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: String, // Corregido de UUID? a String
    val email: String,
    val nombre: String? = null,
    val telefono: String? = null,
    val tipo_plan: String = "gratuito",
    val estado_plan: String = "activo",
    val dias_gratuitos: Int = 7,
    val email_verificado: Boolean = false
)
