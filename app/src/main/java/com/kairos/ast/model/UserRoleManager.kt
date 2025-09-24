package com.kairos.ast.model

import android.content.Context

/**
 * Gestiona el almacenamiento y la recuperación del rol del usuario en SharedPreferences.
 */
object UserRoleManager {

    private const val PREFS_NAME = "KairosUserPrefs"
    private const val KEY_ROLE = "user_role"
    private const val DEFAULT_ROLE = "usuario"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Guarda el rol del usuario en SharedPreferences.
     */
    fun saveRole(context: Context, role: String?) {
        getPrefs(context).edit().putString(KEY_ROLE, role ?: DEFAULT_ROLE).apply()
    }

    /**
     * Obtiene el rol del usuario desde SharedPreferences.
     * @return El rol guardado, o "usuario" por defecto si no hay ninguno.
     */
    fun getRole(context: Context): String {
        return getPrefs(context).getString(KEY_ROLE, DEFAULT_ROLE) ?: DEFAULT_ROLE
    }

    /**
     * Comprueba si el usuario actual tiene el rol de "admin".
     * @return `true` si el rol guardado es "admin", `false` en caso contrario.
     */
    fun isAdmin(context: Context): Boolean {
        return getRole(context).equals("admin", ignoreCase = true)
    }
}
