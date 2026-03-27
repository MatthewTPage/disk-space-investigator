package page.matthewt.diskspaceinvestigator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.FluentTheme
import page.matthewt.diskspaceinvestigator.session.SessionInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionListItem(
    session: SessionInfo,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val dateStr = dateFormat.format(Date(session.lastModified))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.displayName,
                color = FluentTheme.colors.text.text.primary,
            )
            val scannedPart = if (session.scannedSizeBytes > 0) {
                "  •  Scanned: ${SizeDisplay.format(session.scannedSizeBytes)}"
            } else ""
            val durationPart = if (session.scanDurationMillis > 0) {
                "  •  ${SizeDisplay.formatDuration(session.scanDurationMillis)}"
            } else ""
            Text(
                "$dateStr  •  File: ${SizeDisplay.format(session.fileSizeBytes)}$scannedPart$durationPart",
                color = FluentTheme.colors.text.text.secondary,
            )
        }

        Button(onClick = onLoad) {
            Text("Load")
        }
        Button(onClick = onDelete) {
            Text("Delete")
        }
    }
}
