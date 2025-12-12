package mx.edu.utez.data.network

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("sender_id") val senderId: Long,
    @SerializedName("receiver_id") val receiverId: Long,
    @SerializedName("audio_url") val audioUrl: String?,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("text_note") val textNote: String?
)

