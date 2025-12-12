package mx.edu.utez.grabadormultimedia.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Helper para construir el request multipart de registro de usuario.
 * Uso:
 * val helper = RegisterHelper()
 * val username = helper.textPart("miusuario")
 * val email = helper.textPart("correo@ejemplo.com")
 * val password = helper.textPart("micontraseña")
 * val profilePart = helper.filePart("profile_picture", imageFile)
 * val response = RetrofitClient.apiService.register(username, email, password, profilePart)
 */
object RegisterHelper {
    fun textPart(value: String): RequestBody =
        value.toRequestBody("text/plain".toMediaTypeOrNull())

    fun filePart(partName: String, file: File?): MultipartBody.Part? {
        if (file == null || !file.exists()) return null
        val mediaType = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
        val requestBody = file.asRequestBody(mediaType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestBody)
    }
}

