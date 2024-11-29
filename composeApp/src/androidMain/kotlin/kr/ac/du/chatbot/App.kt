package kr.ac.du.chatbot

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.donghoonyoo.langserve.LangServeClient
import com.donghoonyoo.langserve.model.Message
import com.donghoonyoo.langserve.model.MessageSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

val client = LangServeClient(
    https = true,
    hostname = "ollama-endpoint-dsudc2.donghoon.net",
    port = 443,
    path = "eeve-korean-10b"
)

var tts: TextToSpeech? = null

@Composable
fun App() {
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val messageList = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
        }
    }

    fun sendMessage() {
        CoroutineScope(Dispatchers.IO).launch {
            if (isLoading || message.isEmpty()) return@launch
            isLoading = true

            val userMessage = message
            message = ""
            messageList += Message(MessageSpeaker.Human, userMessage)

            val conversation = messageList.toList()
            var aiMessage = Message(MessageSpeaker.AI, "...")
            messageList += aiMessage

            client.stream(conversation).collect { chunk ->
                aiMessage = aiMessage.copy(content = aiMessage.content + chunk)
                messageList[messageList.lastIndex] = aiMessage
            }

            tts?.speak(aiMessage.content, TextToSpeech.QUEUE_FLUSH, null, null)
            isLoading = false
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText =
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                if (!spokenText.isNullOrEmpty()) {
                    message = spokenText
                    sendMessage()
                }
            } else {
                Toast.makeText(context, "음성 인식 실패", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "음성을 입력하세요")
        }
        speechLauncher.launch(intent)
    }
    MaterialTheme {
        Scaffold {innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .align(Alignment.Center),
                    alpha = 0.2f
                )
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Text(
                        text = "DU ChatBot",
                        style = MaterialTheme.typography.h6,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .padding(8.dp)
                            .weight(1f)
                    ) {
                        items(messageList) { msg ->
                            Box(
                                contentAlignment = if (msg.speaker == MessageSpeaker.Human) Alignment.CenterEnd else Alignment.CenterStart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    color = if (msg.speaker == MessageSpeaker.Human) Color.White else Color.Black,
                                    modifier = Modifier
                                        .background(
                                            if (msg.speaker == MessageSpeaker.Human) Color(
                                                0xFF6200EA
                                            )
                                            else Color(0xFFEEEEEE),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { startVoiceInput() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.mic_icon),
                                contentDescription = "Voice Input",
                                modifier = Modifier
                                    .size(36.dp) // 아이콘 크기를 설정
                            )
                        }
                        TextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text("메시지 입력") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                                topStart = 45.dp,  // 왼쪽 위 둥글게
                                topEnd = 45.dp,    // 오른쪽 위 둥글게
                                bottomStart = 45.dp,  // 왼쪽 아래 둥글게
                                bottomEnd = 45.dp    // 오른쪽 아래 둥글게
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                focusedIndicatorColor = Color.Transparent, // 포커스 상태에서 밑줄 제거
                                unfocusedIndicatorColor = Color.Transparent, // 비포커스 상태에서 밑줄 제거
                                disabledIndicatorColor = Color.Transparent // 비활성화 상태에서 밑줄 제거
                            )
                        )
                        IconButton(
                            onClick = { sendMessage() },
                            enabled = message.isNotEmpty() && !isLoading
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.send_icon),
                                contentDescription = "Send",
                                modifier = Modifier
                                    .fillMaxWidth(0.1f) // 화면 너비의 10% 차지
                                    .aspectRatio(1f)

                            )
                        }
                    }
                }
            }
        }
    }
}
