package mx.edu.utez.grabadormultimedia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: Map<String, Any>?
)

