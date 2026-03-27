package page.matthewt.diskspaceinvestigator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.FluentTheme
import page.matthewt.diskspaceinvestigator.ui.theme.AppColors
import page.matthewt.diskspaceinvestigator.session.SessionInfo
import page.matthewt.diskspaceinvestigator.ui.components.SessionListItem
import page.matthewt.diskspaceinvestigator.ui.components.SshConnectDialog
import page.matthewt.diskspaceinvestigator.update.UpdateInfo
import java.io.File
import javax.swing.JFileChooser

@Composable
fun StartScreen(
    sessions: List<SessionInfo>,
    error: String?,
    onLocalScan: (String) -> Unit,
    onSshScan: (host: String, user: String, path: String, port: Int) -> Unit,
    onLoadSession: (File, Long) -> Unit,
    onDeleteSession: (File) -> Unit,
    updateAvailable: UpdateInfo? = null,
    updateProgress: String? = null,
    onInstallUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
) {
    var showSshDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header with logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            @Suppress("DEPRECATION")
            Image(
                painter = painterResource("icon.png"),
                contentDescription = "App icon",
                modifier = Modifier.size(64.dp),
            )
            Column {
                Text(
                    "Disk Space Investigator",
                    style = FluentTheme.typography.title,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                )
                Text(
                    "Analyze disk space usage on local or remote directories",
                    color = AppColors.textSecondary,
                )
            }
        }

        // Error banner
        if (error != null) {
            Text(
                error,
                color = AppColors.error,
            )
        }

        // Update banner
        if (updateAvailable != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.backgroundSecondary, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (updateProgress != null) {
                    com.konyaco.fluent.component.ProgressRing(modifier = Modifier.size(16.dp))
                    Text(updateProgress, color = AppColors.textPrimary, modifier = Modifier.weight(1f))
                } else {
                    Text(
                        "Update available: v${updateAvailable.version}",
                        color = AppColors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onInstallUpdate) {
                        Text("Update")
                    }
                    Button(onClick = onDismissUpdate) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select Directory to Scan"
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    onLocalScan(chooser.selectedFile.absolutePath)
                }
            }) {
                Text("Scan Local Directory")
            }

            Button(onClick = { showSshDialog = true }) {
                Text("Scan Remote Directory (SSH)")
            }
        }

        // Previous sessions
        if (sessions.isNotEmpty()) {
            Text(
                "Previous Sessions",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(sessions) { session ->
                    SessionListItem(
                        session = session,
                        onLoad = { onLoadSession(session.file, session.scanDurationMillis) },
                        onDelete = { onDeleteSession(session.file) },
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
            Text(
                "No previous sessions",
                color = AppColors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.weight(1f))
        }
    }

    // SSH Dialog
    if (showSshDialog) {
        Dialog(onDismissRequest = { showSshDialog = false }) {
            SshConnectDialog(
                onConnect = { host, user, path, port ->
                    showSshDialog = false
                    onSshScan(host, user, path, port)
                },
                onCancel = { showSshDialog = false },
            )
        }
    }
}
