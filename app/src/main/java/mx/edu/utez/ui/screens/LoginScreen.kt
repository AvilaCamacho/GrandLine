package mx.edu.utez.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.edu.utez.viewmodel.LoginViewModel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import mx.edu.utez.grabadormultimedia.data.local.SessionManager
import mx.edu.utez.data.storage.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    val loading by viewModel.loading
    val error by viewModel.error
    val regLoading by viewModel.regLoading
    val regError by viewModel.regError
    var isRegisterMode by remember { mutableStateOf(false) }
    var regLocalError by remember { mutableStateOf<String?>(null) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // copiar a archivo temporal en cache
            CoroutineScope(Dispatchers.IO).launch {
                val f = uriToFile(context, it)
                withContext(Dispatchers.Main) {
                    selectedImageFile = f
                }
            }
        }
    }

    fun isEmailValid(input: String) = Patterns.EMAIL_ADDRESS.matcher(input).matches()
    fun isPasswordValid(input: String) = input.length >= 8

    val tokenManager = TokenManager(context)
    val sessionManager = SessionManager(context)

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (isRegisterMode) "Registrarse" else "Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de usuario") },
                        enabled = !regLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Selector de imagen
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { launcher.launch("image/*") }) {
                            Text("Seleccionar foto de perfil (opcional)")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedImageFile?.name ?: "Sin imagen")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    isError = error != null,
                    enabled = !loading && !regLoading
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    enabled = !loading && !regLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!isRegisterMode) {
                    Button(
                        onClick = {
                            viewModel.login(email, password) { user ->
                                // Guardar sesión por usuario y current user id
                                sessionManager.saveSession(user.id.toInt(), user.email, user.username)
                                tokenManager.saveCurrentUserId(user.id.toInt())
                                onLoginSuccess()
                            }
                        },
                        enabled = !loading
                    ) {
                        Text("Iniciar sesión")
                    }
                } else {
                    Button(
                        onClick = {
                            // Validaciones básicas en cliente
                            when {
                                username.isBlank() -> regLocalError = "El nombre de usuario es obligatorio"
                                !isEmailValid(email) -> regLocalError = "Email inválido"
                                !isPasswordValid(password) -> regLocalError = "La contraseña debe tener al menos 8 caracteres"
                                else -> {
                                    regLocalError = null
                                    // Llamamos al register y manejamos el onSuccess localmente
                                    viewModel.register(username, email, password, selectedImageFile) {
                                        // onSuccess del registro: mostrar Snackbar, volver al modo login
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Registro exitoso")
                                        }
                                        // limpiar campos y volver a login
                                        username = ""
                                        password = ""
                                        email = ""
                                        selectedImageFile = null
                                        isRegisterMode = false
                                    }
                                }
                            }
                        },
                        enabled = !regLoading
                    ) {
                        Text("Registrarse")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                    Text(if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión" else "Crear cuenta")
                }
            }

            if (loading || regLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Mostrar errores de login
            error?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }

            // Mostrar errores de registro desde el servidor
            regError?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearRegisterError() },
                    title = { Text("Error al registrar") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearRegisterError() }) {
                            Text("OK")
                        }
                    }
                )
            }

            // Errores de validación en cliente
            regLocalError?.let { localMsg ->
                AlertDialog(
                    onDismissRequest = { regLocalError = null },
                    title = { Text("Validación") },
                    text = { Text(localMsg) },
                    confirmButton = {
                        Button(onClick = { regLocalError = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

// Helper: copiar Uri a File en cache
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("profile", "", context.cacheDir)
        tempFile.outputStream().use { fileOut ->
            inputStream?.copyTo(fileOut)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
