package page.matthewt.diskspaceinvestigator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.FluentTheme
import page.matthewt.diskspaceinvestigator.model.FileNode

@Composable
fun FileRow(
    node: FileNode,
    onNavigate: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    parentTotalSize: Long,
) {
    val sizeText = SizeDisplay.format(node.totalSize)
    val percentage = if (parentTotalSize > 0) {
        (node.totalSize.toDouble() / parentTotalSize * 100)
    } else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (node.isDirectory) it.clickable { onNavigate() } else it }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon
        Text(
            text = when {
                node.isSymlink -> "\uD83D\uDD17" // link
                node.isAccessDenied -> "\uD83D\uDEAB" // no entry
                node.isDirectory -> "\uD83D\uDCC1" // folder
                else -> "\uD83D\uDCC4" // file
            },
            modifier = Modifier.width(24.dp),
        )

        // Name
        Text(
            text = node.name,
            modifier = Modifier.weight(1f),
            fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (node.isAccessDenied) {
                FluentTheme.colors.text.text.disabled
            } else {
                FluentTheme.colors.text.text.primary
            },
        )

        // Size
        Text(
            text = sizeText,
            modifier = Modifier.width(80.dp),
            fontWeight = FontWeight.Medium,
            color = FluentTheme.colors.text.text.primary,
        )

        // Percentage
        Text(
            text = "%.1f%%".format(percentage),
            modifier = Modifier.width(60.dp),
            color = FluentTheme.colors.text.text.secondary,
        )

        // Actions
        if (!node.isAccessDenied) {
            Button(
                onClick = onOpen,
                modifier = Modifier.height(28.dp),
            ) {
                Text("Open", maxLines = 1)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.height(28.dp),
            ) {
                Text("Delete", maxLines = 1)
            }
        }
    }
}
