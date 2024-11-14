package kr.ac.du.chatbot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.donghoonyoo.langserve.LangServeClient
import com.donghoonyoo.langserve.model.Message
import com.donghoonyoo.langserve.model.MessageSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.util.Locale

val client = LangServeClient(
    https = false,
    hostname = "10.0.2.2",
    port = 8000,
    path = "/chatbot",
)
var tts: TextToSpeech? = null

@Composable
@Preview
fun App() {
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val messageList = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val activity = context as? Activity

    // TextToSpeech 초기화
    tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()  // 앱 종료 시 TextToSpeech 객체 해제
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!spokenText.isNullOrEmpty()) {
                    message = spokenText
                }
            } else {
                Toast.makeText(context, "음성 인식에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "음성으로 메시지를 입력하세요.")
        }
        speechRecognizerLauncher.launch(intent)
    }

    fun sendMessage() {
        CoroutineScope(Dispatchers.IO).launch {
            if (isLoading || message.isEmpty()) return@launch
            isLoading = true

            val copiedMessage = message
            message = ""
            messageList += Message(MessageSpeaker.Human, copiedMessage)

            val messagesWithHistory = messageList.toList()

            var firstChunk = true
            var responseMessage = Message(MessageSpeaker.AI, "...")
            messageList += responseMessage

            client.stream(messagesWithHistory).collect {
                if (firstChunk) {
                    firstChunk = false
                    responseMessage = Message(MessageSpeaker.AI, "")
                }
                responseMessage = Message(MessageSpeaker.AI, responseMessage.content + it)

                messageList.removeLastOrNull()
                messageList += responseMessage
            }

            // AI 응답을 음성으로 출력
            tts?.speak(responseMessage.content, TextToSpeech.QUEUE_FLUSH, null, null)

            isLoading = false
        }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.imePadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .imePadding()
                ) {
                    items(messageList) { message ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            when (message.speaker) {
                                MessageSpeaker.Human -> {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .background(
                                                color = Color(0xFFB39DDB),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = message.content,
                                            color = Color.White,
                                            style = MaterialTheme.typography.body1
                                        )
                                    }
                                }
                                MessageSpeaker.AI -> {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Start)
                                            .background(
                                                color = Color(0xFFE0E0E0),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = message.content,
                                            color = Color.Black,
                                            style = MaterialTheme.typography.body1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { startVoiceInput() },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("🎙️")
                    }

                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Enter text here") },
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = ::sendMessage,
                        enabled = !isLoading && message.isNotEmpty(),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("Submit")
                    }
                }

                LaunchedEffect(messageList.size) {
                    if (messageList.isNotEmpty()) {
                        listState.animateScrollToItem(messageList.size - 1)
                    }
                }
            }
        }
    }
}
