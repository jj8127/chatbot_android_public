package kr.ac.du.chatbot

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
import androidx.compose.ui.unit.dp
import com.donghoonyoo.langserve.LangServeClient
import com.donghoonyoo.langserve.model.Message
import com.donghoonyoo.langserve.model.MessageSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

val client = LangServeClient(
    https = false,
    /**
     * 10.0.2.2 is equivalent for localhost in Android Emulator.
     * Reference: https://developer.android.com/studio/run/emulator-networking.html
     */
    hostname = "10.0.2.2",
    port = 8000,
    path = "/chatbot",
)

@Composable
@Preview
fun App() {
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }  // 입력 텍스트 상태
    val messageList = remember { mutableStateListOf<Message>() } // 메시지와 응답을 저장할 리스트
    val listState = rememberLazyListState() // LazyListState를 기억

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

            isLoading = false
        }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.imePadding() // 자판기가 나타날 때 화면을 밀어올림
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(), // 키보드가 화면을 밀어올리도록 추가
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 저장된 문자열 리스트를 상단에 표시 (스크롤 가능하도록 LazyColumn 사용)
                LazyColumn(
                    state = listState, // LazyListState를 설정
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .imePadding() // LazyColumn이 키보드에 의해 가려지지 않도록 설정
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
                                    // 사용자가 입력한 메시지 출력 (오른쪽 정렬)
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
                                    // 봇의 응답 출력 (왼쪽 정렬)
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

                // 하단에 고정된 입력 창과 버튼 Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 텍스트 입력창
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
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("Submit")
                    }
                }

                // 새로운 메시지가 추가될 때마다 스크롤
                LaunchedEffect(messageList.size) {
                    if (messageList.isNotEmpty()) {
                        listState.animateScrollToItem(messageList.size - 1)
                    }
                }
            }
        }
    }
}