package mx.edu.utez.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.edu.utez.ui.screens.LoginScreen
import mx.edu.utez.ui.screens.HomeScreen
import mx.edu.utez.ui.screens.ChatScreen
import mx.edu.utez.ui.screens.ProfileScreen
import mx.edu.utez.viewmodel.LoginViewModel
import mx.edu.utez.viewmodel.LoginViewModelFactory

@Composable
fun Navigation(loginViewModelFactory: LoginViewModelFactory) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel(factory = loginViewModelFactory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { navController.navigate("home") }
            )
        }
        composable("home") { HomeScreen(navController) }
        composable("chat/{userId}/{username}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: -1L
            val username = backStackEntry.arguments?.getString("username") ?: "Usuario"
            ChatScreen(navController = navController, receiverId = userId, receiverName = username)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }
    }
}