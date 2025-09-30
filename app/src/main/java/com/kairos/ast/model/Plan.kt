package com.kairos.ast.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    val idx: Int,
    val id: String,
    val name: String,
    val description: String,
    val price: String,

    @SerialName("duration_days")
    val durationDays: Int,

    val features: String, // This is a JSON string within a string

    @SerialName("is_popular")
    val isPopular: Boolean,

    @SerialName("is_enabled")
    val isEnabled: Boolean,

    val style: String,

    @SerialName("display_order")
    val displayOrder: Int
)
