package page.matthewt.diskspaceinvestigator.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.darkColors

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    FluentTheme(
        colors = darkColors(),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FluentTheme.colors.background.solid.base)
            ) {
                content()
            }
        }
    }
}
