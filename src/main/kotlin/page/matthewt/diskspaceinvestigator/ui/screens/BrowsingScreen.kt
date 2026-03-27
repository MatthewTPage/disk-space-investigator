package page.matthewt.diskspaceinvestigator.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Text
import page.matthewt.diskspaceinvestigator.ui.theme.AppColors
import page.matthewt.diskspaceinvestigator.model.FileNode
import page.matthewt.diskspaceinvestigator.model.SortMode
import page.matthewt.diskspaceinvestigator.ui.components.DeleteConfirmDialog
import page.matthewt.diskspaceinvestigator.ui.components.FileRow
import page.matthewt.diskspaceinvestigator.ui.components.SizeDisplay

@Composable
fun BrowsingScreen(
    currentNode: FileNode,
    pathStack: List<FileNode>,
    sortMode: SortMode,
    totalFiles: Long,
    totalDirectories: Long,
    totalBytes: Long,
    scanDurationMillis: Long = 0,
    onNavigateInto: (FileNode) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateTo: (Int) -> Unit,
    onToggleSort: () -> Unit,
    onDelete: (FileNode) -> Unit,
    onOpen: (FileNode) -> Unit,
    onSave: () -> Unit,
    onRestart: () -> Unit,
    estimatedSessionSize: Long,
    loadedFromSession: Boolean = false,
    sessionSaved: Boolean = false,
    saving: Boolean = false,
    saveProgress: String? = null,
    deletingPaths: Set<String> = emptySet(),
) {
    var deleteTarget by remember { mutableStateOf<FileNode?>(null) }
    val listState = rememberLazyListState()

    // Cache sorted list to avoid re-sorting on every recomposition
    val sortedChildren by remember(currentNode, sortMode) {
        mutableStateOf(currentNode.sortedBy(sortMode))
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Back button
            if (pathStack.size > 1) {
                Button(onClick = onNavigateUp) {
                    Text("Back")
                }
            }

            // Breadcrumbs
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pathStack.forEachIndexed { index, node ->
                    if (index > 0) {
                        Text(
                            " > ",
                            color = AppColors.textSecondary,
                        )
                    }
                    Text(
                        node.name,
                        color = if (index == pathStack.size - 1) {
                            AppColors.textPrimary
                        } else {
                            AppColors.textSecondary
                        },
                        fontWeight = if (index == pathStack.size - 1) FontWeight.Bold else FontWeight.Normal,
                        modifier = if (index < pathStack.size - 1) {
                            Modifier.clickable { onNavigateTo(index) }
                        } else Modifier,
                    )
                }
            }

            // Actions
            Button(onClick = onToggleSort) {
                Text(
                    when (sortMode) {
                        SortMode.SIZE_DESC -> "Sort: Size"
                        SortMode.NAME_ASC -> "Sort: Name"
                    }
                )
            }

            when {
                loadedFromSession || sessionSaved -> {
                    Text(
                        if (loadedFromSession) "Loaded from session" else "Session saved",
                        color = AppColors.textSecondary,
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp),
                    )
                }
                saving -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        com.konyaco.fluent.component.ProgressRing(modifier = Modifier.size(16.dp))
                        Text(
                            saveProgress ?: "Saving...",
                            color = AppColors.textSecondary,
                        )
                    }
                }
                else -> {
                    Button(onClick = onSave) {
                        Text("Save (~${SizeDisplay.format(estimatedSessionSize)})")
                    }
                }
            }

            Button(onClick = onRestart) {
                Text("Restart")
            }
        }

        // Summary bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                "Current: ${SizeDisplay.format(currentNode.totalSize)}",
                color = AppColors.textSecondary,
            )
            Text(
                "Total: ${SizeDisplay.format(totalBytes)}",
                color = AppColors.textSecondary,
            )
            Text(
                "${SizeDisplay.formatCount(totalFiles)} files, ${SizeDisplay.formatCount(totalDirectories)} dirs",
                color = AppColors.textSecondary,
            )
            if (scanDurationMillis > 0) {
                Text(
                    "Scanned in ${SizeDisplay.formatDuration(scanDurationMillis)}",
                    color = AppColors.textSecondary,
                )
            }
        }

        // Column headers
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.width(24.dp)) // icon space
            Text(
                "Name",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = AppColors.textSecondary,
            )
            Text(
                "Size",
                modifier = Modifier.width(80.dp),
                fontWeight = FontWeight.Bold,
                color = AppColors.textSecondary,
            )
            Text(
                "%",
                modifier = Modifier.width(60.dp),
                fontWeight = FontWeight.Bold,
                color = AppColors.textSecondary,
            )
            Spacer(Modifier.width(140.dp)) // buttons space
        }

        // File list with scrollbar
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
            ) {
                items(sortedChildren, key = { it.absolutePath }) { node ->
                    FileRow(
                        node = node,
                        onNavigate = { onNavigateInto(node) },
                        onOpen = { onOpen(node) },
                        onDelete = { deleteTarget = node },
                        parentTotalSize = currentNode.totalSize,
                        isDeleting = node.absolutePath in deletingPaths,
                    )
                }

                if (sortedChildren.isEmpty()) {
                    item {
                        Text(
                            "Empty directory",
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            color = AppColors.textSecondary,
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(listState),
            )
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { target ->
        Dialog(onDismissRequest = { deleteTarget = null }) {
            DeleteConfirmDialog(
                node = target,
                onConfirm = {
                    onDelete(target)
                    deleteTarget = null
                },
                onCancel = { deleteTarget = null },
            )
        }
    }
}
