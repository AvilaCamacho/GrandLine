package mx.edu.utez.data.network

import com.google.gson.annotations.SerializedName

// Response mínimo. Ajusta según el JSON real de tu API
data class LoginResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: Map<String, Any>?
)

