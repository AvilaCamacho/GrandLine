package mx.edu.utez.grabadormultimedia.data.remote

import mx.edu.utez.grabadormultimedia.data.remote.dto.LoginRequest
import mx.edu.utez.grabadormultimedia.data.remote.dto.LoginResponse
import mx.edu.utez.grabadormultimedia.data.remote.dto.RegisterResponse
import mx.edu.utez.grabadormultimedia.data.remote.dto.MessageResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RemoteDataSource(private val api: ApiService) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val res = api.login(LoginRequest(email, password))
            if (res.isSuccessful) {
                Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string()
                Result.failure(Exception("Login failed: ${res.code()} ${err ?: ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        profilePictureFile: File? = null
    ): Result<RegisterResponse> {
        return try {
            // Para campos de texto multipart es más fiable usar text/plain
            val textMedia = "text/plain".toMediaTypeOrNull()
            val usernameRb = username.toRequestBody(textMedia)
            val emailRb = email.toRequestBody(textMedia)
            val passwordRb = password.toRequestBody(textMedia)

            val picturePart: MultipartBody.Part? = profilePictureFile?.let { file ->
                // Detectar tipo de imagen básico
                val ext = file.extension.lowercase()
                val pictureMedia = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    else -> "application/octet-stream"
                }.toMediaTypeOrNull()
                val reqFile = file.asRequestBody(pictureMedia)
                MultipartBody.Part.createFormData("profile_picture", file.name, reqFile)
            }

            val res = api.register(usernameRb, emailRb, passwordRb, picturePart)
            if (res.isSuccessful) {
                Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string()
                Result.failure(Exception("Register failed: ${res.code()} ${err ?: ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        senderId: RequestBody,
        receiverId: RequestBody,
        audioFile: MultipartBody.Part,
        mediaFile: MultipartBody.Part?,
        textNote: RequestBody?
    ): Result<MessageResponse> {
        return try {
            val res = api.sendMessage(senderId, receiverId, audioFile, mediaFile, textNote)
            if (res.isSuccessful) {
                Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string()
                Result.failure(Exception("Send message failed: ${res.code()} ${err ?: ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<Map<String, Any?>>> {
        return try {
            val res = api.getAllUsers()
            if (res.isSuccessful) {
                Result.success(res.body() ?: emptyList())
            } else {
                val err = res.errorBody()?.string()
                Result.failure(Exception("Get users failed: ${res.code()} ${err ?: ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatMessages(user1Id: Long, user2Id: Long): Result<List<Map<String, Any?>>> {
        return try {
            val res = api.getChatMessages(user1Id, user2Id)
            if (res.isSuccessful) {
                Result.success(res.body() ?: emptyList())
            } else {
                val err = res.errorBody()?.string()
                Result.failure(Exception("Get chat messages failed: ${res.code()} ${err ?: ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
