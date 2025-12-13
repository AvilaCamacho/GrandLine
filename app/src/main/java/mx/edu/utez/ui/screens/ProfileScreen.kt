package mx.edu.utez.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import mx.edu.utez.data.model.User
import mx.edu.utez.data.repository.UserRepository
import mx.edu.utez.grabadormultimedia.data.remote.RemoteDataSource
import mx.edu.utez.grabadormultimedia.data.remote.RetrofitClient
import mx.edu.utez.data.storage.TokenManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val api = RetrofitClient.apiService
    val remote = RemoteDataSource(api)
    val tokenManager = TokenManager(context)
    val repo = UserRepository(remote, tokenManager)

    var loading by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pickedFile by remember { mutableStateOf<File?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var removeSelected by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // copiar a cache file
            val f = uriToFile(context, it)
            pickedFile = f
            pickedUri = it
            removeSelected = false
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        val id = tokenManager.getCurrentUserId().toLong()
        val res = repo.getUserProfile(id)
        loading = false
        if (res.isSuccess) {
            user = res.getOrNull()
            user?.let { u ->
                username = u.username
                email = u.email
            }
        } else {
            error = res.exceptionOrNull()?.message ?: "Error al obtener perfil"
            coroutineScope.launch { snackbarHostState.showSnackbar(error ?: "Error") }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Perfil") }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            if (loading) {
                CircularProgressIndicator()
                return@Column
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(modifier = Modifier.height(12.dp))

            val avatarUrl = pickedUri?.toString() ?: user?.profilePictureUrl
            if (avatarUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUrl),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } else {
                // placeholder
                Box(modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape), contentAlignment = Alignment.Center) {
                    Text(user?.username?.firstOrNull()?.toString() ?: "U")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                Button(onClick = { launcher.launch("image/*") }) { Text("Cambiar foto") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    pickedFile = null
                    pickedUri = null
                    removeSelected = true
                }) { Text("Quitar foto") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Nueva contraseña (opcional)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // llamar actualizar perfil
                val file = pickedFile
                val p = if (file != null && file.exists()) file else null
                coroutineScope.launch {
                    loading = true
                    val res = repo.updateUserProfile(tokenManager.getCurrentUserId().toLong(), username, email, password.ifBlank { null }, p, removeSelected)
                    loading = false
                    if (res.isSuccess) {
                        user = res.getOrNull()
                        navController.popBackStack()
                    } else {
                        val msg = res.exceptionOrNull()?.message ?: "Error al actualizar perfil"
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
            }) {
                Text("Guardar cambios")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nuevo: eliminar cuenta
            Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer), onClick = {
                coroutineScope.launch {
                    val userId = tokenManager.getCurrentUserId().toLong()
                    val res = repo.deleteAccount(userId)
                    if (res.isSuccess) {
                        // navegar a login: limpiar backstack
                        tokenManager.clear()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    } else {
                        val msg = res.exceptionOrNull()?.message ?: "Error al eliminar cuenta"
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
            }) {
                Text("Eliminar cuenta")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { navController.popBackStack() }) { Text("Volver") }
        }
    }
}

// Helper: copiar Uri a File en cache
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("profile", "", context.cacheDir)
        tempFile.outputStream().use { out ->
            inputStream?.copyTo(out)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
