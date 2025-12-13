package mx.edu.utez.grabadormultimedia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("id") val id: Long?,
    @SerializedName("sender_id") val sender_id: Long?,
    @SerializedName("receiver_id") val receiver_id: Long?,
    @SerializedName("audio_url") val audio_url: String?,
    @SerializedName("media_url") val media_url: String?,
    @SerializedName("text_note") val text_note: String?,
    @SerializedName("timestamp") val timestamp: String?
)

