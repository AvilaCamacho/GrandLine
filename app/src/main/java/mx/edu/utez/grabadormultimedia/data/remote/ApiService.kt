package mx.edu.utez.grabadormultimedia.data.remote

import mx.edu.utez.grabadormultimedia.data.remote.dto.LoginRequest
import mx.edu.utez.grabadormultimedia.data.remote.dto.LoginResponse
import mx.edu.utez.grabadormultimedia.data.remote.dto.RegisterResponse
import mx.edu.utez.grabadormultimedia.data.remote.dto.MessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    // Registro con multipart: campos y archivo opcional
    @Multipart
    @POST("/register")
    suspend fun register(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part profile_picture: MultipartBody.Part?
    ): Response<RegisterResponse>

    @Multipart
    @POST("/messages")
    suspend fun sendMessage(
        @Part("sender_id") senderId: RequestBody,
        @Part("receiver_id") receiverId: RequestBody,
        @Part audio_file: MultipartBody.Part,
        @Part media_file: MultipartBody.Part?,
        @Part("text_note") textNote: RequestBody?
    ): Response<MessageResponse>

    // Nuevo endpoint para obtener lista de usuarios desde el servidor
    @GET("/users")
    suspend fun getAllUsers(): Response<List<Map<String, Any?>>>

    // Obtener mensajes entre dos usuarios
    @GET("/messages/{user1_id}/{user2_id}")
    suspend fun getChatMessages(
        @Path("user1_id") user1Id: Long,
        @Path("user2_id") user2Id: Long
    ): Response<List<Map<String, Any?>>>

    @GET("/uploads/{filename}")
    suspend fun downloadUpload(@Path("filename") filename: String): Response<ResponseBody>

    @GET("/media/audio/{message_id}")
    suspend fun downloadAudio(@Path("message_id") messageId: Long): Response<ResponseBody>

    @GET("/media/media/{message_id}")
    suspend fun downloadMedia(@Path("message_id") messageId: Long): Response<ResponseBody>

    // Delete audio from message (preferred method)
    @DELETE("/media/audio/{message_id}")
    suspend fun deleteAudio(
        @Path("message_id") messageId: Long,
        @Header("Authorization") token: String
    ): Response<MessageResponse>

    // Delete audio from message (fallback POST method)
    @POST("/media/audio/{message_id}/delete")
    suspend fun deleteAudioPost(
        @Path("message_id") messageId: Long,
        @Header("Authorization") token: String
    ): Response<MessageResponse>

    // Get user by ID
    @GET("/users/{user_id}")
    suspend fun getUser(
        @Path("user_id") userId: Long,
        @Header("Authorization") token: String
    ): Response<Map<String, Any?>>

    // Update user (preferred PATCH method)
    @Multipart
    @PATCH("/users/{user_id}")
    suspend fun updateUser(
        @Path("user_id") userId: Long,
        @Header("Authorization") token: String,
        @Part("username") username: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("password") password: RequestBody?,
        @Part profile_picture: MultipartBody.Part?
    ): Response<Map<String, Any?>>

    // Update user (fallback PUT method)
    @Multipart
    @PUT("/users/{user_id}")
    suspend fun updateUserPut(
        @Path("user_id") userId: Long,
        @Header("Authorization") token: String,
        @Part("username") username: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("password") password: RequestBody?,
        @Part profile_picture: MultipartBody.Part?
    ): Response<Map<String, Any?>>

    // Update user (fallback POST method)
    @Multipart
    @POST("/users/{user_id}")
    suspend fun updateUserPost(
        @Path("user_id") userId: Long,
        @Header("Authorization") token: String,
        @Part("username") username: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("password") password: RequestBody?,
        @Part profile_picture: MultipartBody.Part?
    ): Response<Map<String, Any?>>
}
