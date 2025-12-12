package mx.edu.utez.data.model

data class Message(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val audioUrl: String?,
    val mediaUrl: String?,
    val textNote: String?,
    val timestamp: String?
)

