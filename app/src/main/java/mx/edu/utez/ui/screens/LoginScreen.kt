package mx.edu.utez.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.edu.utez.R
import mx.edu.utez.viewmodel.LoginViewModel
import mx.edu.utez.grabadormultimedia.data.local.SessionManager
import mx.edu.utez.data.storage.TokenManager
import java.io.File
import java.io.InputStream

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

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF6A1B9A)) // FONDO MORADO
                .padding(paddingValues)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.vetlogo),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRegisterMode) "Registrarse" else "Iniciar sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de usuario", color = Color.White) },
                        enabled = !regLoading,
                        colors = purpleTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White) },
                    isError = error != null,
                    enabled = !loading && !regLoading,
                    colors = purpleTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.White) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    enabled = !loading && !regLoading,
                    colors = purpleTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isRegisterMode) {
                    Button(
                        onClick = {
                            viewModel.login(email, password) { user ->
                                sessionManager.saveSession(
                                    user.id.toInt(),
                                    user.email,
                                    user.username
                                )
                                tokenManager.saveCurrentUserId(user.id.toInt())
                                onLoginSuccess()
                            }
                        },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text("Iniciar sesión", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            when {
                                username.isBlank() -> regLocalError = "El nombre de usuario es obligatorio"
                                !isEmailValid(email) -> regLocalError = "Email inválido"
                                !isPasswordValid(password) -> regLocalError = "La contraseña debe tener al menos 8 caracteres"
                                else -> {
                                    regLocalError = null
                                    viewModel.register(username, email, password, selectedImageFile) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Registro exitoso")
                                        }
                                        username = ""
                                        password = ""
                                        email = ""
                                        selectedImageFile = null
                                        isRegisterMode = false
                                    }
                                }
                            }
                        },
                        enabled = !regLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text("Registrarse", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                    Text(
                        if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión" else "Crear cuenta",
                        color = Color(0xFFE1BEE7)
                    )
                }
            }

            if (loading || regLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            error?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(it) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }

            regError?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearRegisterError() },
                    title = { Text("Error al registrar") },
                    text = { Text(it) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearRegisterError() }) {
                            Text("OK")
                        }
                    }
                )
            }

            regLocalError?.let {
                AlertDialog(
                    onDismissRequest = { regLocalError = null },
                    title = { Text("Validación") },
                    text = { Text(it) },
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

// -------- Helper --------
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("profile", "", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream?.copyTo(output)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// -------- Colors --------
@Composable
private fun purpleTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color(0xFFE1BEE7),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color(0xFFE1BEE7),
    cursorColor = Color.White
)
