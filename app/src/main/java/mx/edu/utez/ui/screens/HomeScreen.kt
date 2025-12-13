@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mx.edu.utez.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import mx.edu.utez.data.repository.UserRepository
import mx.edu.utez.grabadormultimedia.data.remote.RemoteDataSource
import mx.edu.utez.grabadormultimedia.data.remote.RetrofitClient
import mx.edu.utez.data.storage.TokenManager
import mx.edu.utez.viewmodel.HomeViewModel
import mx.edu.utez.viewmodel.HomeViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle

@Composable
fun HomeScreen(navController: NavController) {
    // Crear dependencias mínimas
    val api = RetrofitClient.apiService
    val remote = RemoteDataSource(api)
    val tokenManager = TokenManager(LocalContext.current.applicationContext)
    val repo = UserRepository(remote, tokenManager)
    val factory = HomeViewModelFactory(repo)
    val viewModel: HomeViewModel = viewModel(factory = factory)

    val users by remember { viewModel.users }
    val loading by remember { viewModel.loading }

    LaunchedEffect(Unit) { viewModel.loadUsers() }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Home") },
            actions = {
                IconButton(onClick = { navController.navigate("profile") }) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Perfil")
                }
            }
        )
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    users.forEach { user ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { navController.navigate("chat/${user.id}/${Uri.encode(user.username)}") }, verticalAlignment = Alignment.CenterVertically) {
                            val painter = rememberAsyncImagePainter(user.profilePictureUrl)
                            Image(
                                painter = painter,
                                contentDescription = "avatar",
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.username)
                                Text(user.email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // FAB centrado abajo
                FloatingActionButton(
                    onClick = { /* abrir pantalla nuevo chat */ },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+")
                }
            }
        }
    }
}