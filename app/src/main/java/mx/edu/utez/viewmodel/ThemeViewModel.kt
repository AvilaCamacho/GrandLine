package mx.edu.utez.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.edu.utez.data.SettingsDataStore

class ThemeViewModel : ViewModel() {
    // Estado interno que guarda si está en modo oscuro
    private val _dark = MutableStateFlow(false)
    val dark: StateFlow<Boolean> = _dark

    // si quieres inyectar SettingsDataStore, adapta el constructor
    fun setDarkMode(value: Boolean, settings: SettingsDataStore? = null) {
        viewModelScope.launch {
            _dark.value = value
            settings?.setDarkMode(value)
        }
    }
}