package mx.edu.utez.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.edu.utez.data.repository.UserRepository
import java.io.File
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import mx.edu.utez.data.model.User

class LoginViewModel(private val repo: UserRepository) : ViewModel() {
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _regLoading = mutableStateOf(false)
    val regLoading: State<Boolean> = _regLoading

    private val _regError = mutableStateOf<String?>(null)
    val regError: State<String?> = _regError

    fun login(email: String, password: String, onSuccess: (User) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val res = repo.login(email, password)
            _loading.value = false
            if (res.isSuccess) {
                res.getOrNull()?.let { user ->
                    onSuccess(user)
                }
            } else {
                _error.value = res.exceptionOrNull()?.message ?: "Error"
            }
        }
    }

    fun register(username: String, email: String, password: String, profilePicture: File? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _regLoading.value = true
            val res = repo.register(username, email, password, profilePicture)
            _regLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            } else {
                _regError.value = res.exceptionOrNull()?.message ?: "Error al registrar"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearRegisterError() {
        _regError.value = null
    }
}