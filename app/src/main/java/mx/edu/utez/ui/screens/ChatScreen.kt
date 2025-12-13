package mx.edu.utez.ui.screens

import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.edu.utez.data.model.Message
import mx.edu.utez.data.repository.UserRepository
import mx.edu.utez.grabadormultimedia.data.remote.RemoteDataSource
import mx.edu.utez.grabadormultimedia.data.remote.RetrofitClient
import mx.edu.utez.data.storage.TokenManager
import mx.edu.utez.viewmodel.HomeViewModelFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(receiverId: Long, receiverName: String) {
    val context = LocalContext.current
    val api = RetrofitClient.apiService
    val remote = RemoteDataSource(api)
    val tokenManager = TokenManager(context)
    val currentUserId = tokenManager.getCurrentUserId().toLong()

    // usar repo para mapear
    val repo = UserRepository(remote, tokenManager)

    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recorder: android.media.MediaRecorder? = remember { null }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // lista de mensajes
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var loadingMsgs by remember { mutableStateOf(false) }

    // MediaPlayer para reproducir
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // reproducción: id del mensaje que se está reproduciendo y progreso/duración
    var playingMessageId by remember { mutableStateOf<Long?>(null) }
    val messageDurations = remember { mutableStateMapOf<Long, Int>() } // segundos
    val messageProgress = remember { mutableStateMapOf<Long, Int>() } // segundos elapsed

    val listState = rememberLazyListState()

    fun loadMessages() {
        scope.launch {
            loadingMsgs = true
            val res = repo.getChatMessages(currentUserId, receiverId)
            loadingMsgs = false
            if (res.isSuccess) {
                messages = res.getOrNull() ?: emptyList()
            } else {
                snackbarHost.showSnackbar("Error cargando mensajes")
            }
        }
    }

    fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    // scroll to bottom when messages change
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            // small delay to allow layout
            delay(120)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Permiso runtime
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            scope.launch { snackbarHost.showSnackbar("Permiso de micrófono requerido") }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        loadMessages()
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {}
            try {
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(receiverName) }
        )
    }, snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(8.dp)) {

            if (loadingMsgs) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(messages) { msg ->
                    val isMine = msg.senderId == currentUserId
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
                        Column(modifier = Modifier
                            .padding(8.dp)
                            .widthIn(max = 320.dp)
                            .background(if (isMine) Color(0xFFDCF8C6) else Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)) {

                            msg.textNote?.let { Text(it) }

                            msg.audioUrl?.let { url ->
                                // control play/pause y duración
                                val duration = messageDurations[msg.id] ?: 0
                                val progress = messageProgress[msg.id] ?: 0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = {
                                        scope.launch {
                                            try {
                                                // si ya se está reproduciendo este mensaje -> pause/resume
                                                if (playingMessageId == msg.id && mediaPlayer?.isPlaying == true) {
                                                    mediaPlayer?.pause()
                                                    // detener actualización de progreso
                                                    playingMessageId = msg.id
                                                } else if (playingMessageId == msg.id && mediaPlayer?.isPlaying == false) {
                                                    mediaPlayer?.start()
                                                    playingMessageId = msg.id
                                                } else {
                                                    // reproducir nuevo mensaje
                                                    mediaPlayer?.release()
                                                    mediaPlayer = MediaPlayer().apply {
                                                        setDataSource(url)
                                                        prepareAsync()
                                                        setOnPreparedListener {
                                                            // obtener duración y empezar
                                                            val durMs = it.duration
                                                            val durSec = (durMs / 1000)
                                                            messageDurations[msg.id] = durSec
                                                            messageProgress[msg.id] = 0
                                                            it.start()
                                                            playingMessageId = msg.id
                                                            // actualizar progreso en bucle
                                                            scope.launch {
                                                                while (it.isPlaying) {
                                                                    messageProgress[msg.id] = it.currentPosition / 1000
                                                                    delay(300)
                                                                }
                                                                // al completar
                                                                messageProgress[msg.id] = 0
                                                                playingMessageId = null
                                                            }
                                                        }
                                                        setOnCompletionListener { mp ->
                                                            mp.reset()
                                                            playingMessageId = null
                                                            messageProgress[msg.id] = 0
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                snackbarHost.showSnackbar("Error al reproducir")
                                            }
                                        }
                                    }) {
                                        val isPlaying = playingMessageId == msg.id && mediaPlayer?.isPlaying == true
                                        Text(if (isPlaying) "⏸" else "▶")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        // simple progress text
                                        Text(if (messageDurations[msg.id] ?: 0 > 0) {
                                            "${formatSeconds(messageProgress[msg.id] ?: 0)} / ${formatSeconds(messageDurations[msg.id] ?: 0)}"
                                        } else {
                                            "00:00"
                                        })
                                    }
                                }
                                
                                // Add Delete Audio button (only for own messages)
                                if (isMine) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val res = repo.deleteAudio(msg.id)
                                                    if (res.isSuccess) {
                                                        snackbarHost.showSnackbar("Audio eliminado")
                                                        loadMessages()
                                                    } else {
                                                        val err = res.exceptionOrNull()?.message ?: "Error desconocido"
                                                        // Clean HTML and limit message length
                                                        val clean = err.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                                                        val short = if (clean.length > 200) clean.substring(0, 200) + "..." else clean
                                                        snackbarHost.showSnackbar(short)
                                                        android.util.Log.d("ChatScreen", "Delete audio error full: $err")
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    snackbarHost.showSnackbar("Error: ${e.message}")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("Borrar audio", color = Color.White)
                                    }
                                }
                            }

                            msg.timestamp?.let { Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End) }
                        }
                    }
                }
            }

            // controles grabar / enviar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = {
                    if (!isRecording) {
                        try {
                            val file = File(context.cacheDir, "audio_${UUID.randomUUID()}.mp4")
                            recorder = android.media.MediaRecorder().apply {
                                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                setOutputFile(file.absolutePath)
                                prepare()
                                start()
                            }
                            audioFile = file
                            isRecording = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            recorder?.stop()
                            recorder?.release()
                            recorder = null
                            isRecording = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }) {
                    Text(if (!isRecording) "Grabar" else "Detener")
                }

                Button(onClick = {
                    val file = audioFile
                    if (file == null || !file.exists()) {
                        scope.launch { snackbarHost.showSnackbar("Graba un audio primero") }
                        return@Button
                    }

                    scope.launch {
                        try {
                            val mediaType = "audio/mp4".toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
                            val reqFile = file.asRequestBody(mediaType)
                            val audioPart = MultipartBody.Part.createFormData("audio_file", file.name, reqFile)

                            val senderRb = currentUserId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val receiverRb = receiverId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                            val res = remote.sendMessage(senderRb, receiverRb, audioPart, null, null)
                            if (res.isSuccess) {
                                snackbarHost.showSnackbar("Mensaje enviado")
                                // borrar archivo temporal
                                try { file.delete() } catch (_: Exception) {}
                                audioFile = null
                                // actualizar lista
                                loadMessages()
                            } else {
                                snackbarHost.showSnackbar("Error al enviar: ${res.exceptionOrNull()?.message ?: "unknown"}")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            snackbarHost.showSnackbar("Error: ${e.message}")
                        }
                    }
                }) {
                    Text("Enviar")
                }
            }
        }
    }
}
