package mx.edu.utez.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf
import mx.edu.utez.data.model.User

class PetViewModel : ViewModel() {
    private val _pets = mutableStateListOf<User>() // placeholder: usa model que necesites
    val pets = _pets

    fun loadPets() {
        viewModelScope.launch {
            // Cargar desde repo / API / BD local
            // _pets.addAll(...)
        }
    }
}