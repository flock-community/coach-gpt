import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import chatview.ChatView

@Composable
fun App() {
    MaterialTheme(colors = lightColors(primary = Color.DarkGray)) {
        Surface {
            ChatView()
        }

    }
}

expect fun getPlatformName(): String