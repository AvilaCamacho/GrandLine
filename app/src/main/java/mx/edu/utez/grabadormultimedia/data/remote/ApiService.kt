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
        @Part("text_note") textNote: RequestBody?,
        @Header("Authorization") auth: String? = null
    ): Response<MessageResponse>

    // Nuevo endpoint para obtener lista de usuarios desde el servidor
    @GET("/users")
    suspend fun getAllUsers(): Response<List<Map<String, Any?>>>

    // Obtener un usuario por id
    @GET("/users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: Long, @Header("Authorization") auth: String? = null): Response<Map<String, Any?>>

    // Actualizar perfil de usuario (multipart): username/email/password/profile_picture opcionales
    @Multipart
    @PATCH("/users/{user_id}")
    suspend fun updateUser(
        @Path("user_id") userId: Long,
        @Part("username") username: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("password") password: RequestBody?,
        @Part profile_picture: MultipartBody.Part?,
        @Part("remove_profile") removeProfile: RequestBody?,
        @Header("Authorization") auth: String? = null
    ): Response<Map<String, Any?>>

    // Fallbacks: PUT o POST si PATCH no está permitido en el servidor
    @Multipart
    @PUT("/users/{user_id}")
    suspend fun updateUserPut(
        @Path("user_id") userId: Long,
        @Part("username") username: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("password") password: RequestBody?,
        @Part profile_picture: MultipartBody.Part?,
        @Part("remove_profile") removeProfile: RequestBody?,
        @Header("Authorization") auth: String? = null
    ): Response<Map<String, Any?>>

    @Multipart
    @POST("/users/{user_id}")
    suspend fun updateUserPost(
        @Path("user_id") userId: Long,
        @Part("username") username: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("password") password: RequestBody?,
        @Part profile_picture: MultipartBody.Part?,
        @Part("remove_profile") removeProfile: RequestBody?,
        @Header("Authorization") auth: String? = null
    ): Response<Map<String, Any?>>

    // Obtener mensajes entre dos usuarios
    @GET("/messages/{user1_id}/{user2_id}")
    suspend fun getChatMessages(
        @Path("user1_id") user1Id: Long,
        @Path("user2_id") user2Id: Long,
        @Header("Authorization") auth: String? = null
    ): Response<List<Map<String, Any?>>>

    @GET("/uploads/{filename}")
    suspend fun downloadUpload(@Path("filename") filename: String): Response<ResponseBody>

    @GET("/media/audio/{message_id}")
    suspend fun downloadAudio(@Path("message_id") messageId: Long): Response<ResponseBody>

    @GET("/media/media/{message_id}")
    suspend fun downloadMedia(@Path("message_id") messageId: Long): Response<ResponseBody>

    // PATCH multipart para actualizar un mensaje (texto, media o audio opcionales)
    @Multipart
    @PATCH("/messages/{message_id}")
    suspend fun updateMessage(
        @Path("message_id") messageId: Long,
        @Part("text_note") textNote: RequestBody?,
        @Part media_file: MultipartBody.Part?,
        @Part audio_file: MultipartBody.Part?,
        @Header("Authorization") auth: String? = null
    ): Response<MessageResponse>

    // Eliminar el audio asociado a un mensaje
    @DELETE("/media/audio/{message_id}")
    suspend fun deleteAudio(@Path("message_id") messageId: Long, @Header("Authorization") auth: String? = null): Response<MessageResponse>

    // Fallback: algunos backends usan POST para acciones de borrado
    @POST("/media/audio/{message_id}/delete")
    suspend fun deleteAudioPost(@Path("message_id") messageId: Long, @Header("Authorization") auth: String? = null): Response<MessageResponse>

    // Eliminar cuenta de usuario (DELETE) y fallback POST
    @DELETE("/users/{user_id}")
    suspend fun deleteUser(@Path("user_id") userId: Long, @Header("Authorization") auth: String? = null): Response<Map<String, Any?>>

    @POST("/users/{user_id}/delete")
    suspend fun deleteUserPost(@Path("user_id") userId: Long, @Header("Authorization") auth: String? = null): Response<Map<String, Any?>>
}
