package com.example.superai

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.superai.model.BackendRequest
import com.example.superai.model.ChatMessage
import com.example.superai.network.ApiClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

// --- ViewModel ---
class MainViewModel(private val tts: TextToSpeech) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentAiMode = MutableStateFlow("powerful") // "powerful" or "own_system"
    val currentAiMode: StateFlow<String> = _currentAiMode.asStateFlow()

    fun sendCommand(command: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _messages.value = _messages.value + ChatMessage(text = command, isUser = true)

            val requestBody = BackendRequest(
                prompt = command,
                mode = _currentAiMode.value,
            )
            val backendResponse = ApiClient.send(requestBody)

            if (backendResponse.error != null) {
                val errorMessage = "Error: ${backendResponse.error}"
                _messages.value = _messages.value + ChatMessage(text = errorMessage, isUser = false)
                tts.speak(errorMessage, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                val aiMessage = ChatMessage(
                    text = backendResponse.text ?: "No response text.",
                    isUser = false,
                    imageUrl = backendResponse.images?.firstOrNull(),
                    diagnosticReport = backendResponse.diagnostics?.toString()
                )
                _messages.value = _messages.value + aiMessage
                tts.speak(aiMessage.text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            _isLoading.value = false
        }
    }
    fun setAiMode(mode: String) { _currentAiMode.value = mode }
}

// --- Main Activity ---
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var driveService: Drive

    private val speechRecognizerIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, listOf(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = account.account

                driveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                .setApplicationName("Super AI")
                .build()

                uploadTestFile()
            } catch (e: Exception) {
                Log.e("MainActivity", "Google Sign-In failed", e)
            }
        }
    }

    private fun uploadTestFile() {
        lifecycleScope.launch {
            try {
                val fileMetadata = File().setName("test.txt")
                val fileContent = "Hello Drive".byteInputStream()
                val mediaContent = InputStreamContent("text/plain", fileContent)

                val file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                Log.d("MainActivity", "Uploaded file ID: ${file.id}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error uploading file to Drive", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestIdToken("243517148974-7tuqb901hcdtg6nb8tcji7llrpp1lvgo.apps.googleusercontent.com")
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            val viewModel: MainViewModel = viewModel()
            SuperAIApp(
                viewModel = viewModel,
                onVoiceInput = {
                    speechRecognizer.startListening(speechRecognizerIntent)
                },
                onSyncToDrive = {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}

// --- UI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAIApp(
    viewModel: MainViewModel,
    onVoiceInput: () -> Unit,
    onSyncToDrive: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMode by viewModel.currentAiMode.collectAsState()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Super AI") },
                    actions = {
                        // Settings icon - placeholder for future navigation
                        IconButton(onClick = { /* Navigate to settings */ }) {
                           // Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Powerful Mode")
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = currentMode == "own_system",
                        onCheckedChange = { isChecked ->
                            viewModel.setAiMode(if (isChecked) "own_system" else "powerful")
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Own System Mode")
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(onClick = onSyncToDrive, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Sync Archive to Drive")
                }


                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Type or speak...") }
                    )
                    IconButton(onClick = onVoiceInput) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice Command")
                    }
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.sendCommand(text)
                                text = ""
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = message.text)

                if (!message.imageUrl.isNullOrEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Relevant Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }

                if (!message.isUser) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { isExpanded = !isExpanded }) {
                        Text(if (isExpanded) "Hide Details" else "Show Details")
                    }
                    if (isExpanded) {
                        Column {
                            Text("Model Used: ${message.modelUsed ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                            Text("Diagnostics: ${message.diagnosticReport ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
