package com.kairos.ast.servicios.Versiones

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kairos.ast.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UpdateManager(private val context: Context) {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
    }

    suspend fun checkForUpdates(onUpToDate: () -> Unit) {
        val currentVersion = BuildConfig.VERSION_NAME
        Log.d("UpdateManager", "📲 Versión instalada: $currentVersion")
        try {
            val release = client.get("https://api.github.com/repos/SamiGamin/Kairos/releases/latest")
                .body<JsonObject>()

            val latestVersion = release["tag_name"]?.jsonPrimitive?.content?.removePrefix("v")
            Log.d("UpdateManager", "🌐 Última versión en GitHub: $latestVersion")

            val assets = release["assets"]?.jsonArray
            val apkUrl = assets?.firstOrNull()
                ?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content

            Log.d("UpdateManager", "🔗 APK URL recuperada: $apkUrl")

            if (latestVersion != null && latestVersion != currentVersion) {
                Log.d("UpdateManager", "⚠️ Hay una nueva versión disponible.")
                if (apkUrl != null) {
                    showUpdateDialog(apkUrl)
                    return
                } else {
                    Log.w("UpdateManager", "⚠️ No se encontró un APK adjunto en la release.")
                }
            } else {
                Log.d("UpdateManager", "✅ La app ya está en la última versión.")
            }

            onUpToDate()

        } catch (e: Exception) {
            Log.e("UpdateManager", "❌ Error al consultar GitHub: ${e.message}", e)
            // Si falla la API → dejamos pasar igual
            onUpToDate()
        }
    }

    private fun showUpdateDialog(apkUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Actualización disponible")
            .setMessage("Debes actualizar a la última versión para continuar.")
            .setCancelable(false)
            .setPositiveButton("Actualizar") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                context.startActivity(intent)
            }
            .show()
    }
}