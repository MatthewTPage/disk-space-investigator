package page.matthewt.diskspaceinvestigator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.konyaco.fluent.FluentTheme

object AppColors {
    // Text
    val textPrimary @Composable get() = FluentTheme.colors.text.text.primary
    val textSecondary @Composable get() = FluentTheme.colors.text.text.secondary
    val textDisabled @Composable get() = FluentTheme.colors.text.text.disabled

    // Backgrounds
    val backgroundBase @Composable get() = FluentTheme.colors.background.solid.base
    val backgroundSecondary @Composable get() = FluentTheme.colors.background.solid.secondary

    // Status
    val error = Color(0xFFFF6B6B)
    val success = Color(0xFF1B5E20)
    val textOnStatus = Color.White
}
