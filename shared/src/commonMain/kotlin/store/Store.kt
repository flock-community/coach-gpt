package store

import androidx.compose.runtime.remember
import chatview.isMe
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionCall
import com.aallam.openai.api.chat.FunctionMode
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.ToolChoice
import com.aallam.openai.api.chat.ToolId
import com.aallam.openai.api.chat.toolCall
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import config.Config.API_KEY
import domain.Author
import domain.FollowUp
import domain.Goal
import domain.Message
import domain.Message.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random
import kotlin.reflect.KFunction3


sealed interface Action {
    data class SendMessage(val message: TextMessage) : Action
    data class ReceiveResponse(val content: String) : Action
    data class AddToolCall(val toolCall: ChatMessage) : Action
    data class AddGoal(val goal: Goal, val functionName: String, val functionId: ToolId) : Action
    data class AddFollowUp(
        val followUp: FollowUp,
        val functionName: String,
        val functionId: ToolId
    ) : Action
}

data class ChatState(
    val messages: List<Message> = emptyList<Message>()
//            + listOf(
//        Message("Hi, I'm Coach GPT. I'm here to help you reach your goals", Author.Other("Coach GPT")),
//        Message("That is nice, I want to achieve some goals", Author.Me),
//        Message("I can assist you with that", Author.Other("Coach GPT")),
//        Message("Churros ipsum curly fries bacon, chicken nuggets soda pop. French toast pancake, ground round jalapeño poppers lobster nachos food truck. Brisket chips cotton candy, pepperoni pizza grilled cheese flan tots. Muffin poutine tacos, jambalaya dumplings hot dogs truffle oil popcorn.", Author.Me),
//        Message("Broccoli mac n' cheese pot roast, beef ribs beer margarita. Cheeseburger sushi, tequila lemon pie quesadilla buffalo wing popcorn balls. Ice cream sandwich beef stew, pulled pork chocolate cake jello shot shrimp cocktail. Chimichanga tacos, spicy food bratwurst sliders apple pie cotton candy.", Author.Other("Coach GPT")),
//        Message("Pizza chicken wings, gelato snickers fish and chips molasses cookies. Pretzel cinnamon rolls, banana split corn on the cob popcorn tenderloin. Frosted flakes marshmallow, pumpkin pie honey mustard chow mein canned beans. Mashed potatoes gala apple, potato chips sugar snap peas croissant baklava.", Author.Me),
//    )
)

data class GoalState(
    val goals: List<Goal> = emptyList(),
    val followUps: List<FollowUp> = emptyList()
)

interface Store {
    suspend fun send(action: Action)
    val chatStateFlow: StateFlow<ChatState>
    val goalStateFlow: StateFlow<GoalState>
    val state get() = chatStateFlow.value
    val goalState get() = goalStateFlow.value
}

fun chatReducer(chatState: ChatState, message: Message): ChatState = chatState.copy(
    messages = (chatState.messages + message)
)

fun goalReducer(goalState: GoalState, goal: Goal): GoalState = goalState.copy(
    goals = (goalState.goals + goal)
)

fun followUpReducer(goalState: GoalState, followUp: FollowUp): GoalState = goalState.copy(
    followUps = (goalState.followUps + followUp)
)

fun CoroutineScope.createStore(): Store {
    val mutableChatStateFlow = MutableStateFlow(ChatState())
    val mutableGoalStateFlow = MutableStateFlow(GoalState())

    val random = Random(1)

    val channel: Channel<Action> = Channel(Channel.UNLIMITED)

    val openAI = OpenAI(token = API_KEY, logging = LoggingConfig(LogLevel.All))

    return object : Store {

        init {
            launch {
                channel.consumeAsFlow().collect { action ->

                    when (action) {
                        is Action.SendMessage -> {
                            mutableChatStateFlow.value = chatReducer(state, action.message)
                            sendMessagesToGPT()
                        }

                        is Action.ReceiveResponse -> {
                            mutableChatStateFlow.value = chatReducer(
                                state,
                                TextMessage(action.content, Author.Other("Coach GPT"))
                            )
                        }

                        is Action.AddToolCall -> {
                            mutableChatStateFlow.value = chatReducer(
                                state,
                                ToolCallMessage(action.toolCall)
                            )
                        }

                        is Action.AddGoal -> {
                            mutableGoalStateFlow.value = goalReducer(goalState, action.goal)
                            mutableChatStateFlow.value = chatReducer(
                                state,
                                FunctionMessage(
                                    random.nextInt().toString(),
                                    "Added a goal: '${action.goal.name}' - '${action.goal.description}'",
                                    action.functionName,
                                    action.functionId.id
                                ),

                                )
                            sendMessagesToGPT()
                        }

                        is Action.AddFollowUp -> {
                            mutableGoalStateFlow.value = followUpReducer(goalState, action.followUp)
                            mutableChatStateFlow.value = chatReducer(
                                state,
                                FunctionMessage(
                                    random.nextInt().toString(),
                                    "Added a follow-up after '${action.followUp.after}' from now",
                                    action.functionName,
                                    action.functionId.id
                                )
                            )
                            sendMessagesToGPT()
                        }
                    }
                }
            }
        }

        private suspend fun sendMessagesToGPT() {
            val message =
                openAI.chatCompletion(generateRequest(state.messages)).choices.first().message

            message.content?.let {
                send(Action.ReceiveResponse(it))
            }

            message.toolCalls?.let {
                send(Action.AddToolCall(message))

                // TODO The order of messages is not guaranteed, the AddToolCall needs to be exucuted before
                //delay(1000)
                it.forEach { toolCall ->
                    send(toolCall.execute())
                }
            }

        }

        override suspend fun send(action: Action) {
            launch {
                channel.send(action)
            }.join()
        }


        override val chatStateFlow: StateFlow<ChatState> = mutableChatStateFlow
        override val goalStateFlow: StateFlow<GoalState> = mutableGoalStateFlow
    }
}

private val availableFunctions: Map<String, KFunction3<JsonObject, String, ToolId, Action>> = mapOf(
    Tools.addOrUpdateGoal.function.name to ::addOrUpdateGoal,
    Tools.scheduleFollowupMeeting.function.name to ::scheduleFollowUp,
)

private fun addOrUpdateGoal(args: JsonObject, functionName: String, functionId: ToolId): Action {

    val id = args["goalId"]?.jsonPrimitive?.content ?: error("id not found")
    val name = args["goalName"]?.jsonPrimitive?.content ?: error("name not found")
    val description =
        args["goalDescription"]?.jsonPrimitive?.content ?: error("description not found")

    return Action.AddGoal(Goal(id, name, description), functionName, functionId)
}

private fun scheduleFollowUp(args: JsonObject, functionName: String, functionId: ToolId): Action {

    val followUpAfter =
        args["followUpAfter"]?.jsonPrimitive?.content ?: error("followUpAfter not found")

    return Action.AddFollowUp(FollowUp(followUpAfter), functionName, functionId)
}

private fun ToolCall.execute(): Action {
    val functionToCall =
        availableFunctions[function.name] ?: error("Function ${function.name} not found")
    val functionArgs = function.argumentsAsJson()
    return functionToCall(functionArgs, function.name, id)
}

fun generateRequest(messages: List<Message>): ChatCompletionRequest {
    val chatCompletionRequest = ChatCompletionRequest(
        model = ModelId("gpt-4-1106-preview"),
        messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = "You are an AI personal development coach called 'Coach-GPT', powered by GPT. Your purpose is to assist users in setting, tracking, and achieving their personal development goals. You engage users in meaningful conversations, asking insightful questions to understand their goals, progress, and any obstacles they're facing. You provide constructive feedback, celebrate their milestones, and suggest innovative strategies to overcome challenges. The interaction with the user is through a chat app, therefore please keep your questions short and don't ask multiple bullet point questions in one message, keep it interactive. When you've identified a goal you register it, when you need to update a goal please use the same goalId. When you know enough about a goal you schedule a follow-up meeting to evaluate the progress of the goal, this can be for example in 2 weeks from now"
            )
        ) + messages.map {
            ChatMessage(
                toolCallId = when (it) {
                    is FunctionMessage -> ToolId(it.toolCallId)
                    is TextMessage -> null
                    is ToolCallMessage -> it.toolCall.toolCallId
                },
                role = when (it) {
                    is FunctionMessage -> ChatRole.Tool
                    is TextMessage -> if (it.isMe()) ChatRole.User else ChatRole.System
                    is ToolCallMessage -> it.toolCall.role
                },
                content = when (it) {
                    is FunctionMessage -> it.text
                    is TextMessage -> it.text
                    is ToolCallMessage -> it.toolCall.content
                },
                name = when (it) {
                    is FunctionMessage -> it.functionName
                    is TextMessage -> null
                    is ToolCallMessage -> it.toolCall.name
                },
                toolCalls = when (it) {
                    is ToolCallMessage -> it.toolCall.toolCalls
                    else -> null
                },
            )
        },
        tools = listOf(Tools.addOrUpdateGoal, Tools.scheduleFollowupMeeting),
        toolChoice = ToolChoice.Auto
    )

    return chatCompletionRequest
}
