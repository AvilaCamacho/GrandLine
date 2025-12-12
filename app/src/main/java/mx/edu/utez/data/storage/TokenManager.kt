package mx.edu.utez.data.storage

import android.content.Context

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("auth_token", null)

    fun clear() {
        prefs.edit().remove("auth_token").apply()
    }

    // Guardar id del usuario actual para uso interno (no confidencial) y permitir ChatScreen obtener sender_id
    fun saveCurrentUserId(id: Int) {
        prefs.edit().putInt("current_user_id", id).apply()
    }

    fun getCurrentUserId(): Int = prefs.getInt("current_user_id", -1)
}
