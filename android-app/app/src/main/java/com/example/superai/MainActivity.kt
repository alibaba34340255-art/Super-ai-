package com.example.superai

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.superai.data.BackendRequest
import com.example.superai.data.ChatMessage
import com.example.superai.network.ApiClient
import kotlinx.coroutines.launch
import java.util.*

// --- ViewModel ---
class MainViewModel(private val tts: TextToSpeech) : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    private val _currentAiMode = mutableStateOf("powerful") // "powerful" or "own_system"
    val currentAiMode: State<String> = _currentAiMode

    // Placeholder for user-defined API key from settings
    private val customGeminiApiKey = mutableStateOf<String?>(null) // e.g., "YOUR_GEMINI_API_KEY"

    fun sendCommand(command: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _messages.add(ChatMessage(text = command, isUser = true)) // Add user message
            try {
                val requestBody = BackendRequest(
                    prompt = command,
                    mode = _currentAiMode.value,
                    custom_api_key = customGeminiApiKey.value
                )
                val backendResponse = ApiClient.send(requestBody)
                val payload = backendResponse.response

                val aiMessage = ChatMessage(
                    text = payload.text,
                    isUser = false,
                    imageUrl = payload.image_url,
                    modelUsed = backendResponse.model_used,
                    diagnosticReport = backendResponse.diagnostic_report
                )
                _messages.add(aiMessage) // Add AI message
                tts.speak(payload.text, TextToSpeech.QUEUE_FLUSH, null, null)

            } catch (e: Exception) {
                val errorMsg = "Error: Could not connect to backend. ${e.message}"
                _messages.add(ChatMessage(text = errorMsg, isUser = false))
                tts.speak(errorMsg, TextToSpeech.QUEUE_FLUSH, null, null)
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun setAiMode(mode: String) { _currentAiMode.value = mode }
}

// --- Main Activity ---
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val speechRecognizerIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            val viewModel = MainViewModel(tts)
            SuperAIApp(viewModel = viewModel, onVoiceInput = {
                speechRecognizer.startListening(speechRecognizerIntent)
            })
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
fun SuperAIApp(viewModel: MainViewModel, onVoiceInput: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val isLoading = viewModel.isLoading.value
    val currentMode = viewModel.currentAiMode.value

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
                // Mode Toggle Switch
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

                // Messages Display
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // "Sync to Drive" Button - Placeholder
                Button(onClick = { /* TODO: Implement file sharing logic */ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Sync Archive to Drive")
                }


                Spacer(Modifier.height(8.dp))

                // Input Row
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

                // Display image if URL exists
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

                // Researcher Panel for AI messages
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
