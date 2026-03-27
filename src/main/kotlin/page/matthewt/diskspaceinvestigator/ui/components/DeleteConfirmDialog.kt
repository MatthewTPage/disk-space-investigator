package page.matthewt.diskspaceinvestigator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.FluentTheme
import page.matthewt.diskspaceinvestigator.model.FileNode

@Composable
fun DeleteConfirmDialog(
    node: FileNode,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(400.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FluentTheme.colors.background.solid.secondary)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Permanently Delete",
            style = FluentTheme.typography.subtitle,
            fontWeight = FontWeight.Bold,
        )

        Text(
            "This will permanently delete and cannot be undone:",
            color = FluentTheme.colors.text.text.secondary,
        )

        Column(
            modifier = Modifier.padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Name: ${node.name}", fontWeight = FontWeight.SemiBold)
            Text("Size: ${SizeDisplay.format(node.totalSize)}")
            if (node.isDirectory) {
                Text("Files: ${SizeDisplay.formatCount(node.fileCount)}")
                Text("Directories: ${SizeDisplay.formatCount(node.directoryCount)}")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = onConfirm) {
                Text("Delete Permanently")
            }
        }
    }
}
