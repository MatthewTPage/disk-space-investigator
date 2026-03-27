package page.matthewt.diskspaceinvestigator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.ProgressRing
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.FluentTheme
import page.matthewt.diskspaceinvestigator.ui.theme.AppColors
import page.matthewt.diskspaceinvestigator.model.ScanProgress
import page.matthewt.diskspaceinvestigator.model.ScanSource
import page.matthewt.diskspaceinvestigator.ui.components.SizeDisplay

@Composable
fun ScanningScreen(
    progress: ScanProgress,
    source: ScanSource,
    startTimeMillis: Long,
    onCancel: () -> Unit,
) {
    val elapsed = remember(progress) {
        val millis = System.currentTimeMillis() - startTimeMillis
        val seconds = millis / 1000
        val minutes = seconds / 60
        if (minutes > 0) "${minutes}m ${seconds % 60}s" else "${seconds}s"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ProgressRing(modifier = Modifier.size(64.dp))

        Spacer(Modifier.height(24.dp))

        Text(
            "Scanning ${source.displayName}",
            style = FluentTheme.typography.subtitle,
            color = AppColors.textPrimary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(16.dp))

        // Stats
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Phase: ${progress.phase.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = AppColors.textSecondary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatItem("Files", SizeDisplay.formatCount(progress.filesScanned))
                StatItem("Directories", SizeDisplay.formatCount(progress.directoriesScanned))
                StatItem("Size", SizeDisplay.format(progress.bytesTotal))
                StatItem("Elapsed", elapsed)
            }

            if (progress.currentPath.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 60.dp)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Text(
                        progress.currentPath,
                        color = AppColors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Spacer(Modifier.height(60.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = FluentTheme.typography.bodyStrong,
            color = AppColors.textPrimary,
        )
        Text(
            label,
            color = AppColors.textSecondary,
        )
    }
}
