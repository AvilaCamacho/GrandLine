// ...existing code...
package mx.edu.utez.grabadormultimedia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: Map<String, Any>? = null,
    @SerializedName("token") val token: String? = null
)
// ...existing code...

