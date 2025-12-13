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
    
    /**
     * Helper function to try API calls with different Authorization header variants
     * (with and without "Bearer " prefix)
     */
    private suspend fun <T> tryWithAuthVariants(
        token: String,
        block: suspend (String) -> Response<T>
    ): Response<T> {
        // Try with "Bearer " prefix first
        val tokenWithBearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val res1 = try {
            block(tokenWithBearer)
        } catch (e: Exception) {
            null
        }
        
        if (res1 != null && res1.isSuccessful) {
            return res1
        }
        
        // Try without "Bearer " prefix as fallback
        val tokenWithoutBearer = token.removePrefix("Bearer ")
        return block(tokenWithoutBearer)
    }
    
    /**
     * Clean HTML tags and normalize whitespace from error messages
     */
    private fun cleanErrorMessage(message: String): String {
        // Remove HTML tags and normalize whitespace
        // Using proper Kotlin string escaping with double backslash
        val cleaned = message
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned
    }
    
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

    /**
     * Delete audio from a message
     * Tries DELETE method first, then POST fallback
     */
    suspend fun deleteAudio(messageId: Long, token: String): Result<MessageResponse> {
        val errors = mutableListOf<String>()
        
        // Try DELETE method first
        try {
            val res = tryWithAuthVariants(token) { authToken ->
                api.deleteAudio(messageId, authToken)
            }
            if (res.isSuccessful && res.body() != null) {
                return Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                errors.add("DELETE: $err")
            }
        } catch (e: Exception) {
            errors.add("DELETE: ${e.message}")
        }
        
        // Try POST fallback
        try {
            val res = tryWithAuthVariants(token) { authToken ->
                api.deleteAudioPost(messageId, authToken)
            }
            if (res.isSuccessful && res.body() != null) {
                return Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                errors.add("POST: $err")
            }
        } catch (e: Exception) {
            errors.add("POST: ${e.message}")
        }
        
        // All methods failed, return cleaned error message
        val joined = errors.joinToString(" | ")
        val cleaned = cleanErrorMessage(joined)
        return Result.failure(Exception("Delete audio failed: $cleaned"))
    }

    /**
     * Update user profile
     * Tries PATCH method first, then PUT, then POST fallback
     */
    suspend fun updateUser(
        userId: Long,
        token: String,
        username: String?,
        email: String?,
        password: String?,
        profilePictureFile: File?
    ): Result<Map<String, Any?>> {
        val textMedia = "text/plain".toMediaTypeOrNull()
        val usernameRb = username?.toRequestBody(textMedia)
        val emailRb = email?.toRequestBody(textMedia)
        val passwordRb = password?.toRequestBody(textMedia)
        
        val picturePart: MultipartBody.Part? = profilePictureFile?.let { file ->
            val ext = file.extension.lowercase()
            val pictureMedia = when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }.toMediaTypeOrNull()
            val reqFile = file.asRequestBody(pictureMedia)
            MultipartBody.Part.createFormData("profile_picture", file.name, reqFile)
        }
        
        val errors = mutableListOf<String>()
        
        // Try PATCH method first
        try {
            val res = tryWithAuthVariants(token) { authToken ->
                api.updateUser(userId, authToken, usernameRb, emailRb, passwordRb, picturePart)
            }
            if (res.isSuccessful && res.body() != null) {
                return Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                errors.add("PATCH: $err")
            }
        } catch (e: Exception) {
            errors.add("PATCH: ${e.message}")
        }
        
        // Try PUT fallback
        try {
            val res = tryWithAuthVariants(token) { authToken ->
                api.updateUserPut(userId, authToken, usernameRb, emailRb, passwordRb, picturePart)
            }
            if (res.isSuccessful && res.body() != null) {
                return Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                errors.add("PUT: $err")
            }
        } catch (e: Exception) {
            errors.add("PUT: ${e.message}")
        }
        
        // Try POST fallback
        try {
            val res = tryWithAuthVariants(token) { authToken ->
                api.updateUserPost(userId, authToken, usernameRb, emailRb, passwordRb, picturePart)
            }
            if (res.isSuccessful && res.body() != null) {
                return Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                errors.add("POST: $err")
            }
        } catch (e: Exception) {
            errors.add("POST: ${e.message}")
        }
        
        // All methods failed, return cleaned error message
        val joined = errors.joinToString(" | ")
        val cleaned = cleanErrorMessage(joined)
        return Result.failure(Exception("Update user failed: $cleaned"))
    }
}
