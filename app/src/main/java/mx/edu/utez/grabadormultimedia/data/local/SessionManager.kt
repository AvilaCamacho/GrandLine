package mx.edu.utez.grabadormultimedia.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager guarda los datos de sesión por usuario en SharedPreferences
 * Cada usuario tendrá su propio archivo de prefs: "session_<userId>" para aislar datos.
 */
class SessionManager(private val context: Context) {
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_USERNAME = "username"
    }

    private fun prefsFileNameForUser(userId: Int) = "session_\${userId}"

    fun saveSession(userId: Int, email: String, username: String) {
        val prefs = context.getSharedPreferences(prefsFileNameForUser(userId), Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getSession(userId: Int): Map<String, Any?> {
        val prefs = context.getSharedPreferences(prefsFileNameForUser(userId), Context.MODE_PRIVATE)
        return mapOf(
            KEY_USER_ID to prefs.getInt(KEY_USER_ID, -1),
            KEY_EMAIL to prefs.getString(KEY_EMAIL, null),
            KEY_USERNAME to prefs.getString(KEY_USERNAME, null)
        )
    }

    fun clearSession(userId: Int) {
        val prefs = context.getSharedPreferences(prefsFileNameForUser(userId), Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

