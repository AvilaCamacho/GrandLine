package mx.edu.utez.data.network

import mx.edu.utez.grabadormultimedia.data.remote.dto.RegisterResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Login con DTO body
    @POST("login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    // Registro con multipart: campos y archivo opcional
    @Multipart
    @POST("register")
    suspend fun register(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part profile_picture: MultipartBody.Part?
    ): Response<RegisterResponse>

    // Enviar mensaje con archivo de audio y opcional media (multipart)
    @Multipart
    @POST("messages")
    suspend fun sendMessage(
        @Part("sender_id") senderId: RequestBody,
        @Part("receiver_id") receiverId: RequestBody,
        @Part audio_file: MultipartBody.Part,
        @Part media_file: MultipartBody.Part?,
        @Part("text_note") textNote: RequestBody?
    ): Response<MessageResponse>

    // Descargar archivos (ejemplos)
    @GET("uploads/{filename}")
    suspend fun downloadUpload(@Path("filename") filename: String): Response<ResponseBody>

    @GET("media/audio/{message_id}")
    suspend fun downloadAudio(@Path("message_id") messageId: Long): Response<ResponseBody>

    @GET("media/media/{message_id}")
    suspend fun downloadMedia(@Path("message_id") messageId: Long): Response<ResponseBody>
}