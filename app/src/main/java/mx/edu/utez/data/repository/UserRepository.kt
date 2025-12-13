package mx.edu.utez.data.repository

import mx.edu.utez.data.model.User
import mx.edu.utez.data.model.Message
import mx.edu.utez.grabadormultimedia.data.remote.RemoteDataSource
import mx.edu.utez.data.storage.TokenManager
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
            val res = remote.getChatMessages(user1Id, user2Id)
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
                Result.failure(Exception("GetChatMessages failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete audio from a message
     */
    suspend fun deleteAudio(messageId: Long): Result<Message> {
        return try {
            val token = tokenManager?.getToken() ?: return Result.failure(Exception("No token available"))
            val res = remote.deleteAudio(messageId, token)
            if (res.isSuccess) {
                val msgResp = res.getOrNull()
                if (msgResp != null) {
                    // Map MessageResponse to Message
                    val message = Message(
                        id = msgResp.id ?: 0L,
                        senderId = msgResp.sender_id ?: 0L,
                        receiverId = msgResp.receiver_id ?: 0L,
                        audioUrl = msgResp.audio_url,
                        mediaUrl = msgResp.media_url,
                        textNote = msgResp.text_note,
                        timestamp = msgResp.timestamp
                    )
                    Result.success(message)
                } else {
                    Result.failure(Exception("No message returned"))
                }
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Delete audio failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}