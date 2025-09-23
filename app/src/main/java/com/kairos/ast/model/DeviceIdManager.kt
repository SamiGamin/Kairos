package com.kairos.ast.model

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class Dispositivo(
    val device_id_hash: String,
    val usuario_id: String
)

object DeviceIdManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "kairos_device_id_key"
    private const val PREFS_NAME = "device_prefs"
    private const val PREFS_DEVICE_ID_KEY = "encrypted_device_id"
    private const val TAG = "DeviceIdManager"

    /**
     * Obtiene un ID único y hasheado del dispositivo de forma segura.
     * El ID se genera una vez y se guarda cifrado para usos futuros.
     */
    fun getSecureDeviceId(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val encryptedId = prefs.getString(PREFS_DEVICE_ID_KEY, null)

            if (encryptedId != null) {
                decryptDeviceId(encryptedId)
            } else {
                val newDeviceId = generateAndHashDeviceId(context)
                val encrypted = encryptDeviceId(newDeviceId)
                prefs.edit().putString(PREFS_DEVICE_ID_KEY, encrypted).apply()
                newDeviceId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo el ID seguro. Usando fallback.", e)
            val fallbackId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "fallback_${System.currentTimeMillis()}"
            sha256Hash(fallbackId) // Hashear también el fallback para consistencia
        }
    }

    /**
     * Genera un ID único basado en varias fuentes y le aplica un hash SHA-256.
     */
    private fun generateAndHashDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_android_id"

        val buildInfo = "${android.os.Build.MANUFACTURER}:${android.os.Build.MODEL}:${android.os.Build.BOARD}"
        val combined = "$androidId:$buildInfo"

        return sha256Hash(combined)
    }

    /**
     * Cifra el device ID hash usando Android Keystore.
     */
    private fun encryptDeviceId(deviceIdHash: String): String {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encryptedBytes = cipher.doFinal(deviceIdHash.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw RuntimeException("Error cifrando device ID", e)
        }
    }

    /**
     * Descifra el device ID hash.
     */
    private fun decryptDeviceId(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Error descifrando device ID", e)
        }
    }

    /**
     * Obtiene o crea la clave de cifrado en Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
             .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
             .setKeySize(256)
             .build()
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Verifica si el hash del dispositivo ya ha sido registrado en Supabase.
     */
    suspend fun hasDeviceUsedFreeTrial(deviceIdHash: String): Boolean {
        return try {
            val result = SupabaseClient.client
                .from("dispositivos")
                .select {
                    filter {
                        eq("device_id_hash", deviceIdHash)
                    }
                    limit(1)
                }
            // En esta versión de la librería, el resultado es un String JSON.
            // Verificamos si el string no es nulo y no es un array vacío "[]".
            !result.data.isNullOrBlank() && result.data != "[]"
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar el dispositivo en Supabase. Asumiendo que ya fue usado.", e)
            true // Fail-closed
        }
    }

    /**
     * Registra un nuevo dispositivo en Supabase, asociándolo a un usuario.
     */
    suspend fun registrarDispositivo(deviceIdHash: String, userId: String) {
        try {
            val nuevoDispositivo = Dispositivo(
                device_id_hash = deviceIdHash,
                usuario_id = userId
            )
            // La función `insert` se llama directamente sobre el resultado de `from()`
            SupabaseClient.client.from("dispositivos").insert(listOf(nuevoDispositivo))
            Log.i(TAG, "Dispositivo registrado exitosamente para el usuario $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar el dispositivo para el usuario $userId", e)
        }
    }

    /**
     * Función hash SHA-256.
     */
    private fun sha256Hash(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}