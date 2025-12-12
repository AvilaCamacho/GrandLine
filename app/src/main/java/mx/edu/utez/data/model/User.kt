package mx.edu.utez.data.model

data class User(
    val id: Long = 0,
    val email: String,
    val username: String,
    val profilePictureUrl: String? = null
)