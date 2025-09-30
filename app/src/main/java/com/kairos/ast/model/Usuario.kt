package com.kairos.ast.model

import kotlinx.serialization.Serializable
import java.time.Instant


@Serializable
data class Usuario(
    val id: String,                 // uuid
    val email: String,
    val nombre: String? = null,
    val avatar_url: String? = null, // URL de la imagen de perfil
    val telefono: String? = null,
    val rol: String = "usuario",          // Rol del usuario (admin, usuario)
    val tipo_plan: String = "gratuito",   // por defecto gratuito
    val estado_plan: String = "activo",   // activo al registrarse
    @Serializable(with = InstantSerializer::class)
    val fecha_registro: Instant,        // timestamptz en Supabase
    @Serializable(with = InstantSerializer::class)
    val fecha_expiracion_plan: Instant,    // fin del trial
    val dias_Plan: Int,
    val email_verificado: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val ultimo_login: Instant? = null,

    @Serializable(with = InstantSerializer::class)
    val created_at: Instant? = null,

    @Serializable(with = InstantSerializer::class)
    val updated_at: Instant? = null
)