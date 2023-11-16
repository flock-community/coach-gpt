package domain

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ToolCall

sealed interface Author {
    data object Me : Author
    data class Other(val name: String) : Author
}

sealed interface Message {

    data class TextMessage(val text: String, val author: Author) : Message

    data class ToolCallMessage(val toolCall: ChatMessage) : Message

    data class FunctionMessage(
        val id: String,
        val text: String,
        val functionName: String,
        val toolCallId: String
    ) : Message
}

data class Goal(
    val id: String,
    val name: String,
    val description: String,
)

data class FollowUp(
    val after: String
)