package com.kairos.ast.servicios.Versiones

import android.app.Activity
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

// Data class para encapsular la información de la release de GitHub
private data class GithubRelease(val versionName: String, val apkUrl: String?)

class UpdateManager(private val context: Context) {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
    }

    /**
     * Orquesta la comprobación de actualizaciones.
     * Si encuentra una, muestra un diálogo y detiene el flujo.
     * Si no, o si hay un error, ejecuta la lambda onUpToDate para continuar.
     */
    suspend fun checkForUpdates(onUpToDate: () -> Unit) {
        val currentVersion = BuildConfig.VERSION_NAME
        Log.d("UpdateManager", "📲 Versión instalada: $currentVersion")

        val latestRelease = getLatestReleaseFromGitHub()

        if (latestRelease == null) {
            Log.e("UpdateManager", "No se pudo obtener la información de la última versión. Dejando continuar.")
            onUpToDate()
            return
        }

        Log.d("UpdateManager", "🌐 Última versión en GitHub: ${latestRelease.versionName}")

        if (latestRelease.versionName != currentVersion) {
            Log.d("UpdateManager", "⚠️ Hay una nueva versión disponible.")
            if (latestRelease.apkUrl != null) {
                showUpdateDialog(latestRelease.apkUrl)
                // El flujo se detiene aquí, no se llama a onUpToDate()
            } else {
                Log.w("UpdateManager", "⚠️ Nueva versión encontrada, pero sin APK adjunto. Dejando continuar.")
                onUpToDate()
            }
        } else {
            Log.d("UpdateManager", "✅ La app ya está en la última versión.")
            onUpToDate()
        }
    }

    /**
     * Realiza la llamada a la API de GitHub para obtener los datos de la última release.
     * @return Un objeto [GithubRelease] con la información o null si hay un error.
     */
    private suspend fun getLatestReleaseFromGitHub(): GithubRelease? {
        return try {
            val releaseJson = client.get("https://api.github.com/repos/SamiGamin/Kairos/releases/latest")
                .body<JsonObject>()

            val latestVersion = releaseJson["tag_name"]?.jsonPrimitive?.content?.removePrefix("v")
            val apkUrl = releaseJson["assets"]?.jsonArray
                ?.firstOrNull()
                ?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content

            if (latestVersion != null) {
                GithubRelease(versionName = latestVersion, apkUrl = apkUrl)
            } else {
                Log.w("UpdateManager", "El JSON de la release de GitHub no contiene 'tag_name'.")
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "❌ Error al consultar GitHub: ${e.message}", e)
            null
        }
    }

    /**
     * Muestra el diálogo de actualización, solo si el contexto es una Activity válida y no se está cerrando.
     */
    private fun showUpdateDialog(apkUrl: String) {
        if (context is Activity && !context.isFinishing) {
            AlertDialog.Builder(context)
                .setTitle("Actualización disponible")
                .setMessage("Debes actualizar a la última versión para continuar.")
                .setCancelable(false)
                .setPositiveButton("Actualizar") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                    context.startActivity(intent)
                }
                .show()
        } else {
            Log.w("UpdateManager", "No se puede mostrar el diálogo de actualización, el contexto no es una Activity válida o se está destruyendo.")
        }
    }
}