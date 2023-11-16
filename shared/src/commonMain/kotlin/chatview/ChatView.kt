package chatview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aallam.openai.api.chat.Tool
import domain.Author
import domain.Message.*
import general.Colors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import store.Action
import store.Store
import store.createStore


fun TextMessage.isMe() = author is Author.Me


@Composable
fun ToolCallMessageCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp))
            .background(Color.LightGray)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                fontSize = 14.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FunctionMessageCard(message: FunctionMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp))
            .background(Color.LightGray)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                fontSize = 14.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun TextMessageCard(message: TextMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isMe()) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(modifier = Modifier.padding(all = 5.dp)) {

            if (!message.isMe()) {
                Box(Modifier.align(Alignment.Bottom)) {  // Wrap with a Box and use align
                    Column(Modifier) {
                        Image(
                            painterResource("help.png"),
                            "contact profile description",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Box(
                    Modifier.clip(
                        RoundedCornerShape(
                            10.dp,
                            10.dp,
                            if (!message.isMe()) 10.dp else 0.dp,
                            if (!message.isMe()) 0.dp else 10.dp
                        )
                    )
                        .background(color = if (!message.isMe()) Colors.OTHERS_MESSAGE_BACKGROUND else Colors.MY_MESSAGE_BACKGROUND)
                        .padding(start = 10.dp, top = 5.dp, end = 10.dp, bottom = 5.dp),
                ) {
                    Column {
                        if (!message.isMe()) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                (message.author as? Author.Other)?.let {
                                    Text(
                                        text = it.name,
                                        style = MaterialTheme.typography.body1.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.sp,
                                            fontSize = 14.sp
                                        ),
                                        color = Colors.OTHERS_MESSAGE_AUTHOR
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.size(3.dp))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.body1.copy(
                                fontSize = 18.sp,
                                letterSpacing = 0.sp
                            ),
                            color = if (!message.isMe()) Colors.OTHERS_MESSAGE_TEXT else Colors.MY_MESSAGE_TEXT
                        )
                        Spacer(Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}


@OptIn(DelicateCoroutinesApi::class)
@Composable
fun ChatView() {

    val store = CoroutineScope(SupervisorJob()).createStore()

    val chatState by store.chatStateFlow.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val messagesFlow = snapshotFlow { chatState.messages }

    LaunchedEffect(messagesFlow) {
        messagesFlow.collectLatest {
            coroutineScope.launch {
                val lastIndex = it.size - 1
                if (lastIndex >= 0) {
                    listState.animateScrollToItem(lastIndex)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.linearGradient(Colors.BLACK_GRADIENT))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            GoalBar(store)

            Box(Modifier.weight(1f).fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp).align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState,
                ) {

                    item { Spacer(Modifier.size(20.dp)) }
                    items(chatState.messages, key = { when(it) {
                        is FunctionMessage -> it.id
                        is TextMessage -> it.text
                        is ToolCallMessage -> (0..1000000000).random()
                    } }) {
                        when(it) {
                            is FunctionMessage -> FunctionMessageCard(it)
                            is TextMessage -> TextMessageCard(it)
                            is ToolCallMessage -> ToolCallMessageCard("Executing action...")
                        }

                    }
                }
            }
            SendMessage { text ->
                GlobalScope.launch {
                    store.send(Action.SendMessage(TextMessage(text, Author.Me)))
                }
            }
        }
    }
}

@Composable
fun GoalBar(store: Store) {

    val goalState by store.goalStateFlow.collectAsState()

    Box(
        modifier = Modifier.height(80.dp).fillMaxWidth().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AppBarButton("goal.png", goalState.goals.size) {
                println("Clicked on goals")
            }
            AppBarButton("schedule.png", goalState.followUps.size) {
                println("Clicked on follow ups")
            }
        }
    }
    Divider(Modifier.fillMaxWidth().height(1.dp), Color.White)
}

@Composable
fun Counter(count: Int) {
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.body1.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontSize = 16.sp
        ),
        color = Color.White,
        modifier = Modifier.padding(3.dp)
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AppBarButton(image: String, count: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick
    ) {
        Image(
            painterResource(image),
            "",
            modifier = Modifier
                .size(40.dp)
        )
        Counter(count)
    }
}

@Composable
fun SendMessage(sendMessage: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    TextField(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(10.dp),
        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White),
        value = inputText,
        placeholder = {
            Text("Type message...")
        },
        onValueChange = {
            inputText = it
        },
        trailingIcon = {
            if (inputText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clickable {
                            sendMessage(inputText)
                            inputText = ""
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colors.primary
                    )
                    Text("Send")
                }
            }
        }
    )
}

