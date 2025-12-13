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

    // Helper para probar auth con y sin Bearer si 401
    private suspend fun <T> tryWithAuthVariants(callWithAuth: suspend (String?) -> retrofit2.Response<T>, auth: String?): Result<retrofit2.Response<T>> {
        return try {
            val initial = callWithAuth(auth)
            if (initial.isSuccessful) return Result.success(initial)
            if (initial.code() == 401 && auth != null) {
                // probar sin Bearer
                val alt = if (auth.startsWith("Bearer ")) auth.removePrefix("Bearer ") else "Bearer $auth"
                val second = callWithAuth(alt)
                if (second.isSuccessful) return Result.success(second)
                // devolver el primero como fallback con detalles
                return Result.failure(Exception("HTTP ${initial.code()} - ${initial.errorBody()?.string() ?: ""}"))
            }
            val err = initial.errorBody()?.string()
            Result.failure(Exception("HTTP ${initial.code()} - ${err ?: ""}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Enviar mensaje con auth opcional
    suspend fun sendMessage(
        senderId: RequestBody,
        receiverId: RequestBody,
        audioFile: MultipartBody.Part,
        mediaFile: MultipartBody.Part?,
        textNote: RequestBody?,
        auth: String? = null
    ): Result<MessageResponse> {
        return try {
            val resResult = tryWithAuthVariants({ a -> api.sendMessage(senderId, receiverId, audioFile, mediaFile, textNote, a) }, auth)
            if (resResult.isSuccess) {
                val r = resResult.getOrNull()!!
                Result.success(r.body()!!)
            } else {
                Result.failure(resResult.exceptionOrNull()!!)
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

    // Nuevo: obtener usuario raw (con auth opcional) con variantes
    suspend fun getUser(userId: Long, auth: String? = null): Result<Map<String, Any?>> {
        val tryRes = tryWithAuthVariants({ a -> api.getUser(userId, a) }, auth)
        return try {
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body() ?: emptyMap())
            }
            Result.failure(tryRes.exceptionOrNull() ?: Exception("Get user failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sobracarga: actualizar usuario aceptando Strings y File (más cómodo) con auth
    suspend fun updateUser(
        userId: Long,
        username: String? = null,
        email: String? = null,
        password: String? = null,
        profileFile: File? = null,
        removeProfile: Boolean = false,
        auth: String? = null
    ): Result<Map<String, Any?>> {
        return try {
            val textMedia = "text/plain".toMediaTypeOrNull()
            val usernameRb = username?.toRequestBody(textMedia)
            val emailRb = email?.toRequestBody(textMedia)
            val passwordRb = password?.toRequestBody(textMedia)
            val removeRb = if (removeProfile) "true".toRequestBody(textMedia) else null

            val picturePart: MultipartBody.Part? = profileFile?.let { file ->
                val ext = file.extension.lowercase()
                val media = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    else -> "application/octet-stream"
                }.toMediaTypeOrNull()
                val req = file.asRequestBody(media)
                MultipartBody.Part.createFormData("profile_picture", file.name, req)
            }

            val errors = mutableListOf<String>()

            // Intentar POST primero
            val postRes = tryWithAuthVariants({ a -> api.updateUserPost(userId, usernameRb, emailRb, passwordRb, picturePart, removeRb, a) }, auth)
            if (postRes.isSuccess) {
                val r3 = postRes.getOrNull()!!
                if (r3.isSuccessful) return Result.success(r3.body() ?: emptyMap())
                errors.add("POST ${r3.code()} ${r3.errorBody()?.string()}")
            } else {
                errors.add(postRes.exceptionOrNull()?.message ?: "POST failed")
            }

            // Luego PATCH
            val tryRes = tryWithAuthVariants({ a -> api.updateUser(userId, usernameRb, emailRb, passwordRb, picturePart, removeRb, a) }, auth)
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body() ?: emptyMap())
                errors.add("PATCH ${r.code()} ${r.errorBody()?.string()}")
            } else {
                errors.add(tryRes.exceptionOrNull()?.message ?: "PATCH failed")
            }

            // Finalmente PUT
            val putRes = tryWithAuthVariants({ a -> api.updateUserPut(userId, usernameRb, emailRb, passwordRb, picturePart, removeRb, a) }, auth)
            if (putRes.isSuccess) {
                val r2 = putRes.getOrNull()!!
                if (r2.isSuccessful) return Result.success(r2.body() ?: emptyMap())
                errors.add("PUT ${r2.code()} ${r2.errorBody()?.string()}")
            } else {
                errors.add(putRes.exceptionOrNull()?.message ?: "PUT failed")
            }

            Result.failure(Exception("Update user failed: " + errors.joinToString(" | ")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: actualizar usuario raw (con auth)
    suspend fun updateUser(
        userId: Long,
        username: RequestBody?,
        email: RequestBody?,
        password: RequestBody?,
        profilePart: MultipartBody.Part?,
        removeProfile: RequestBody?,
        auth: String? = null
    ): Result<Map<String, Any?>> {
        val tryRes = tryWithAuthVariants({ a -> api.updateUser(userId, username, email, password, profilePart, removeProfile, a) }, auth)
        return try {
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body() ?: emptyMap())
            }
            Result.failure(tryRes.exceptionOrNull() ?: Exception("Update user failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener chat messages con auth opcional
    suspend fun getChatMessages(user1Id: Long, user2Id: Long, auth: String? = null): Result<List<Map<String, Any?>>> {
        val tryRes = tryWithAuthVariants({ a -> api.getChatMessages(user1Id, user2Id, a) }, auth)
        return try {
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body() ?: emptyList())
            }
            Result.failure(tryRes.exceptionOrNull() ?: Exception("Get chat messages failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // PATCH multipart para actualizar un mensaje (texto, media o audio opcionales) con auth
    suspend fun updateMessage(
        messageId: Long,
        textNote: String? = null,
        mediaFile: File? = null,
        audioFile: File? = null,
        auth: String? = null
    ): Result<MessageResponse> {
        return try {
            val textRb: RequestBody? = textNote?.toRequestBody("text/plain".toMediaTypeOrNull())

            val mediaPart: MultipartBody.Part? = mediaFile?.let { file ->
                val ext = file.extension.lowercase()
                val mediaMedia = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "mp4", "m4v" -> "video/mp4"
                    else -> "application/octet-stream"
                }.toMediaTypeOrNull()
                val req = file.asRequestBody(mediaMedia)
                MultipartBody.Part.createFormData("media_file", file.name, req)
            }

            val audioPart: MultipartBody.Part? = audioFile?.let { file ->
                val ext = file.extension.lowercase()
                val audioMedia = when (ext) {
                    "mp3" -> "audio/mpeg"
                    "m4a", "mp4" -> "audio/mp4"
                    "wav" -> "audio/wav"
                    else -> "application/octet-stream"
                }.toMediaTypeOrNull()
                val req = file.asRequestBody(audioMedia)
                MultipartBody.Part.createFormData("audio_file", file.name, req)
            }

            val tryRes = tryWithAuthVariants({ a -> api.updateMessage(messageId, textRb, mediaPart, audioPart, a) }, auth)
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body()!!)
            }
            Result.failure(tryRes.exceptionOrNull() ?: Exception("Update message failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: eliminar audio de un mensaje con auth
    suspend fun deleteAudio(messageId: Long, auth: String? = null): Result<MessageResponse> {
        return try {
            val errors = mutableListOf<String>()
            // Intentar DELETE primero
            val tryRes = tryWithAuthVariants({ a -> api.deleteAudio(messageId, a) }, auth)
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body()!!)
                errors.add("DELETE ${r.code()} ${r.errorBody()?.string()}")
            } else {
                errors.add(tryRes.exceptionOrNull()?.message ?: "DELETE failed")
            }

            // Fallback: intentar POST delete
            val postTry = tryWithAuthVariants({ a -> api.deleteAudioPost(messageId, a) }, auth)
            if (postTry.isSuccess) {
                val r2 = postTry.getOrNull()!!
                if (r2.isSuccessful) return Result.success(r2.body()!!)
                errors.add("DELETE-POST ${r2.code()} ${r2.errorBody()?.string()}")
            } else {
                errors.add(postTry.exceptionOrNull()?.message ?: "DELETE-POST failed")
            }

            // Normalizar mensaje de error: remover tags HTML si vienen
            val joined = errors.joinToString(" | ")
            val cleaned = joined.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            Result.failure(Exception("Delete audio failed: $cleaned"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Eliminar cuenta de usuario (DELETE) con fallback POST
    suspend fun deleteUser(userId: Long, auth: String? = null): Result<Map<String, Any?>> {
        return try {
            val errors = mutableListOf<String>()
            // Intentar DELETE
            val tryRes = tryWithAuthVariants({ a -> api.deleteUser(userId, a) }, auth)
            if (tryRes.isSuccess) {
                val r = tryRes.getOrNull()!!
                if (r.isSuccessful) return Result.success(r.body() ?: emptyMap())
                errors.add("DELETE ${r.code()} ${r.errorBody()?.string()}")
            } else {
                errors.add(tryRes.exceptionOrNull()?.message ?: "DELETE failed")
            }

            // Fallback POST
            val postTry = tryWithAuthVariants({ a -> api.deleteUserPost(userId, a) }, auth)
            if (postTry.isSuccess) {
                val r2 = postTry.getOrNull()!!
                if (r2.isSuccessful) return Result.success(r2.body() ?: emptyMap())
                errors.add("POST-DELETE ${r2.code()} ${r2.errorBody()?.string()}")
            } else {
                errors.add(postTry.exceptionOrNull()?.message ?: "POST-DELETE failed")
            }

            val joined = errors.joinToString(" | ")
            val cleaned = joined.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            Result.failure(Exception("Delete user failed: $cleaned"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
