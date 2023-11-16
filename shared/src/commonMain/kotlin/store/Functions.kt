package store

import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.core.Parameters
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject


object Tools {

    val addOrUpdateGoal =
        Tool.function(
            name = "addOrUpdateGoal",
            description = "Adds the user goal if it does not exist, if a goal with the same goalId exists, it will be updated",
            parameters = Parameters.buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("goalId") {
                        put("type", "string")
                        put("description", "The identifier of the goal")
                    }
                    putJsonObject("goalName") {
                        put("type", "string")
                        put("description", "A short name identifying the goal")
                    }
                    putJsonObject("goalDescription") {
                        put("type", "string")
                        put("description", "A description of the goal")
                    }
                }
                putJsonArray("required") {
                    add("goalId")
                    add("goalName")
                    add("goalDescription")
                }
            })

    val scheduleFollowupMeeting =
        Tool.function(
            name = "scheduleFollowUpMeeting",
            description = "Plan a follow up meeting in the future. This moment indicates in how many time you (Coach-gpt) want to speak to the user again. This moment is based on what time you think is appropriate to check in with the user again. When this time has elapsed the user will receive a notification and will continue with this conversation",
            Parameters.buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("followUpAfter") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add("1_HOUR")
                            add("1_DAY")
                            add("1_WEEK")
                            add("2_WEEK")
                            add("1_MONTH")
                        }
                    }
                }
                putJsonArray("required") {
                    add("followUpAfter")
                }
            })

}
