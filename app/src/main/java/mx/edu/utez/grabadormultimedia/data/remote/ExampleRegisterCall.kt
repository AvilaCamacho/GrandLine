package mx.edu.utez.grabadormultimedia.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File

suspend fun exampleRegisterCall(usernameStr: String, emailStr: String, passwordStr: String, profileImage: File?) {
    withContext(Dispatchers.IO) {
        try {
            val username: RequestBody = usernameStr.toRequestBody("text/plain".toMediaTypeOrNull())
            val email: RequestBody = emailStr.toRequestBody("text/plain".toMediaTypeOrNull())
            val password: RequestBody = passwordStr.toRequestBody("text/plain".toMediaTypeOrNull())
            val profilePart = RegisterHelper.filePart("profile_picture", profileImage)

            val response = RetrofitClient.apiService.register(username, email, password, profilePart)
            if (response.isSuccessful) {
                Log.d("Register", "Registro ok: ${response.body()}")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("Register", "Error HTTP ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e("Register", "Excepción al registrar: ${e.message}")
        }
    }
}
