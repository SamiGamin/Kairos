package com.kairos.ast.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/**
 * Serializer para java.time.Instant en formato ISO-8601 (ej: 2025-09-23T20:15:30Z)
 */
object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        // Lo guarda como String ISO-8601
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        // Lo lee como String y lo convierte a Instant
        return Instant.parse(decoder.decodeString())
    }
}