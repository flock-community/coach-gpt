package domain

sealed interface Author {
    data object Me : Author
    data class Other(val name: String) : Author
}

sealed interface Message {

    val text: String

    data class TextMessage(override val text: String, val author: Author) : Message

    data class FunctionMessage(val id: String, override val text: String, val functionName: String) : Message
}



data class Goal(
    val id: String,
    val name: String,
    val description: String,
)

data class FollowUp(
    val after: String
)