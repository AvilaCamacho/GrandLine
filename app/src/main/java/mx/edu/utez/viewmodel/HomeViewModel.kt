package mx.edu.utez.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.edu.utez.data.model.User
import mx.edu.utez.data.repository.UserRepository

class HomeViewModel(private val userRepository: UserRepository) : ViewModel() {
    val users = mutableStateOf<List<User>>(emptyList())
    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun loadUsers() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                val res = userRepository.getAllUsers()
                loading.value = false
                if (res.isSuccess) {
                    users.value = res.getOrNull() ?: emptyList()
                } else {
                    error.value = res.exceptionOrNull()?.message ?: "Error al cargar usuarios"
                }
            } catch (e: Exception) {
                loading.value = false
                error.value = e.message ?: "Error"
            }
        }
    }
}
