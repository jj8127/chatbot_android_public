package kr.ac.du.chatbot

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var message by remember { mutableStateOf("") }  // 입력 텍스트 상태
        val text_list = remember { mutableStateListOf<Pair<String, String>>() } // 메시지와 응답을 저장할 리스트
        val listState = rememberLazyListState() // LazyListState를 기억

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
                    items(text_list) { (savedMessage, answer) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                                    text = savedMessage,
                                    color = Color.White,
                                    style = MaterialTheme.typography.body1
                                )
                            }

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
                                    text = answer,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.body1
                                )
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
                        onClick = {
                            if (message.isNotEmpty()) {
                                text_list.add(Pair(message, "$message answer"))
                                message = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("Submit")
                    }
                }

                // 새로운 메시지가 추가될 때마다 스크롤
                LaunchedEffect(text_list.size) {
                    if (text_list.isNotEmpty()) {
                        listState.animateScrollToItem(text_list.size - 1)
                    }
                }
            }
        }
    }
}