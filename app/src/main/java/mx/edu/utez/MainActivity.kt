package mx.edu.utez

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utez.data.repository.UserRepository
import mx.edu.utez.data.storage.TokenManager
import mx.edu.utez.grabadormultimedia.data.remote.RetrofitClient
import mx.edu.utez.grabadormultimedia.data.remote.RemoteDataSource
import mx.edu.utez.ui.Navigation
import mx.edu.utez.viewmodel.LoginViewModelFactory
import mx.edu.utez.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Crear ApiClient y RemoteDataSource
        val api = RetrofitClient.apiService
        val remote = RemoteDataSource(api)
        val tokenManager = TokenManager(applicationContext)
        val userRepo = UserRepository(remote, tokenManager)
        val loginFactory = LoginViewModelFactory(userRepo)

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val isDark by themeViewModel.dark.collectAsState()

            Log.d(TAG, "isDarkTheme = $isDark")

            MaterialTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    //abre directamente la navegacion
                    Navigation(loginViewModelFactory = loginFactory)
                }
            }
        }
    }
}
