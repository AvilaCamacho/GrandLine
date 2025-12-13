package mx.edu.utez.data.repository

import mx.edu.utez.data.model.User
import mx.edu.utez.data.model.Message
import mx.edu.utez.grabadormultimedia.data.remote.RemoteDataSource
import mx.edu.utez.data.storage.TokenManager
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class UserRepository(private val remote: RemoteDataSource, private val tokenManager: TokenManager?) {
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val res = remote.login(email, password)
            if (res.isSuccess) {
                val body = res.getOrNull()
                // Mapear LoginResponse.user (Map) a User: intenta extraer campos básicos
                val userMap = body?.user
                val id = (userMap?.get("id") as? Double)?.toLong() ?: 1L
                val emailResp = userMap?.get("email") as? String ?: email
                val username = userMap?.get("username") as? String ?: (userMap?.get("name") as? String ?: "Usuario")
                // Guardar token si viene
                body?.token?.let { token ->
                    tokenManager?.saveToken(token)
                }
                Result.success(User(id = id, email = emailResp, username = username))
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String, profilePicture: File? = null): Result<User> {
        return try {
            val res = remote.register(username, email, password, profilePicture)
            if (res.isSuccess) {
                val body = res.getOrNull()
                // Guardar token si viene
                body?.token?.let { token ->
                    tokenManager?.saveToken(token)
                }
                val userMap = body?.user
                val id = (userMap?.get("id") as? Double)?.toLong() ?: 1L
                val emailResp = userMap?.get("email") as? String ?: email
                val usernameResp = userMap?.get("username") as? String ?: username
                Result.success(User(id = id, email = emailResp, username = usernameResp))
            } else {
                Result.failure(Exception("Register failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val res = remote.getAllUsers()
            if (res.isSuccess) {
                val listMap = res.getOrNull() ?: emptyList()
                val users = listMap.mapNotNull { map ->
                    try {
                        val id = when (val v = map["id"]) {
                            is Double -> v.toLong()
                            is Int -> v.toLong()
                            is Long -> v
                            is Number -> v.toLong()
                            else -> 0L
                        }
                        val email = map["email"] as? String ?: ""
                        val username = map["username"] as? String ?: map["name"] as? String ?: "Usuario"
                        val profile = map["profile_picture_url"] as? String
                        User(id = id, email = email, username = username, profilePictureUrl = profile)
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(users)
            } else {
                Result.failure(Exception("GetAllUsers failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatMessages(user1Id: Long, user2Id: Long): Result<List<Message>> {
        return try {
            val token = tokenManager?.getToken()?.let { "Bearer $it" }
            val res = remote.getChatMessages(user1Id, user2Id, token)
            if (res.isSuccess) {
                val listMap = res.getOrNull() ?: emptyList()
                val messages = listMap.mapNotNull { map ->
                    try {
                        val id = when (val v = map["id"]) {
                            is Double -> v.toLong()
                            is Int -> v.toLong()
                            is Long -> v
                            is Number -> v.toLong()
                            else -> 0L
                        }
                        val sender = when (val v = map["sender_id"]) {
                            is Double -> v.toLong()
                            is Int -> v.toLong()
                            is Long -> v
                            is Number -> v.toLong()
                            else -> 0L
                        }
                        val receiver = when (val v = map["receiver_id"]) {
                            is Double -> v.toLong()
                            is Int -> v.toLong()
                            is Long -> v
                            is Number -> v.toLong()
                            else -> 0L
                        }
                        val audioUrl = map["audio_url"] as? String
                        val mediaUrl = map["media_url"] as? String
                        val textNote = map["text_note"] as? String
                        val timestamp = map["timestamp"] as? String
                        Message(
                            id = id,
                            senderId = sender,
                            receiverId = receiver,
                            audioUrl = audioUrl,
                            mediaUrl = mediaUrl,
                            textNote = textNote,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(messages)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("GetChatMessages failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: enviar mensaje pasando token
    suspend fun sendMessage(
        senderId: RequestBody,
        receiverId: RequestBody,
        audioFile: MultipartBody.Part,
        mediaFile: MultipartBody.Part?,
        textNote: RequestBody?
    ): Result<Message> {
        return try {
            val token = tokenManager?.getToken()?.let { "Bearer $it" }
            val res = remote.sendMessage(senderId, receiverId, audioFile, mediaFile, textNote, token)
            if (res.isSuccess) {
                val body = res.getOrNull()
                val message = Message(
                    id = body?.id ?: 0L,
                    senderId = body?.senderId ?: 0L,
                    receiverId = body?.receiverId ?: 0L,
                    audioUrl = body?.audioUrl,
                    mediaUrl = body?.mediaUrl,
                    textNote = body?.textNote,
                    timestamp = null
                )
                Result.success(message)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("SendMessage failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: exponer updateMessage a capas superiores
    suspend fun updateMessage(
        messageId: Long,
        textNote: String? = null,
        mediaFile: File? = null,
        audioFile: File? = null
    ): Result<Message> {
        return try {
            val res = remote.updateMessage(messageId, textNote, mediaFile, audioFile)
            if (res.isSuccess) {
                val body = res.getOrNull()
                // Mapear MessageResponse a Message
                val message = Message(
                    id = body?.id ?: 0L,
                    senderId = body?.senderId ?: 0L,
                    receiverId = body?.receiverId ?: 0L,
                    audioUrl = body?.audioUrl,
                    mediaUrl = body?.mediaUrl,
                    textNote = body?.textNote,
                    timestamp = null
                )
                Result.success(message)
            } else {
                Result.failure(Exception("UpdateMessage failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: exponer deleteAudio
    suspend fun deleteAudio(messageId: Long): Result<Message> {
        return try {
            val token = tokenManager?.getToken()?.let { "Bearer $it" }
            val res = remote.deleteAudio(messageId, token)
            if (res.isSuccess) {
                val body = res.getOrNull()
                val message = Message(
                    id = body?.id ?: 0L,
                    senderId = body?.senderId ?: 0L,
                    receiverId = body?.receiverId ?: 0L,
                    audioUrl = body?.audioUrl,
                    mediaUrl = body?.mediaUrl,
                    textNote = body?.textNote,
                    timestamp = null
                )
                Result.success(message)
            } else {
                // Propagar detalle del error
                Result.failure(res.exceptionOrNull() ?: Exception("DeleteAudio failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: obtener perfil (wrapper)
    suspend fun getUserProfile(userId: Long): Result<User> {
        return try {
            val token = tokenManager?.getToken()?.let { "Bearer $it" }
            val res = remote.getUser(userId, token)
            if (res.isSuccess) {
                val map = res.getOrNull() ?: emptyMap()
                val id = when (val v = map["id"]) {
                    is Double -> v.toLong()
                    is Int -> v.toLong()
                    is Long -> v
                    is Number -> v.toLong()
                    else -> 0L
                }
                val email = map["email"] as? String ?: ""
                val username = map["username"] as? String ?: map["name"] as? String ?: "Usuario"
                val profile = map["profile_picture_url"] as? String
                Result.success(User(id = id, email = email, username = username, profilePictureUrl = profile))
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("GetUserProfile failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: actualizar perfil (wrapper)
    suspend fun updateUserProfile(
        userId: Long,
        username: String? = null,
        email: String? = null,
        password: String? = null,
        profileFile: File? = null,
        removeProfile: Boolean = false
    ): Result<User> {
        return try {
            val token = tokenManager?.getToken()?.let { "Bearer $it" }
            val res = remote.updateUser(userId, username, email, password, profileFile, removeProfile, token)
            if (res.isSuccess) {
                val map = res.getOrNull() ?: emptyMap()
                val id = when (val v = map["id"]) {
                    is Double -> v.toLong()
                    is Int -> v.toLong()
                    is Long -> v
                    is Number -> v.toLong()
                    else -> 0L
                }
                val emailResp = map["email"] as? String ?: ""
                val usernameResp = map["username"] as? String ?: map["name"] as? String ?: "Usuario"
                val profile = map["profile_picture_url"] as? String
                Result.success(User(id = id, email = emailResp, username = usernameResp, profilePictureUrl = profile))
            } else {
                val ex = res.exceptionOrNull() ?: Exception("UpdateUserProfile failed: unknown")
                Result.failure(ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nuevo: eliminar cuenta del usuario
    suspend fun deleteAccount(userId: Long): Result<Boolean> {
        return try {
            val token = tokenManager?.getToken()?.let { "Bearer $it" }
            val res = remote.deleteUser(userId, token)
            if (res.isSuccess) {
                // borrar token y current user
                tokenManager?.clear()
                Result.success(true)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("DeleteAccount failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}