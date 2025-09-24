package com.kairos.ast.ui.planes.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa un plan de suscripción. Los nombres de los campos están anotados
 * con @SerialName para coincidir con las columnas de la base de datos de Supabase.
 */
@Serializable
data class Plan(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("price")
    val price: String,
    @SerialName("duration_days")
    val durationDays: Int,
    @SerialName("features")
    val features: List<String>,
    @SerialName("is_popular")
    val isPopular: Boolean = false,
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
    @SerialName("style")
    val style: String = "NORMAL",
    @SerialName("display_order")
    val displayOrder: Int = 0,
    @SerialName("price_subtitle")
    val priceSubtitle: String? = null
)