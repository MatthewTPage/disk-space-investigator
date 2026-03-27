package page.matthewt.diskspaceinvestigator.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.FluentTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import page.matthewt.diskspaceinvestigator.ui.screens.BrowsingScreen
import page.matthewt.diskspaceinvestigator.ui.screens.ScanningScreen
import page.matthewt.diskspaceinvestigator.ui.screens.StartScreen
import page.matthewt.diskspaceinvestigator.ui.theme.AppTheme
import page.matthewt.diskspaceinvestigator.viewmodel.AppState
import page.matthewt.diskspaceinvestigator.viewmodel.AppViewModel
import page.matthewt.diskspaceinvestigator.viewmodel.AuthPrompt

@Composable
fun App(viewModel: AppViewModel) {
    AppTheme {
        val state by viewModel.state.collectAsState()
        val authPrompt by viewModel.authPrompt.collectAsState()
        val error by viewModel.error.collectAsState()
        val saveMessage by viewModel.saveMessage.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentState = state) {
                is AppState.Start -> StartScreen(
                    sessions = currentState.sessions,
                    error = currentState.error,
                    onLocalScan = { viewModel.startLocalScan(it) },
                    onSshScan = { host, user, path, port ->
                        viewModel.startSshScan(host, user, path, port)
                    },
                    onLoadSession = { file, duration -> viewModel.loadSession(file, duration) },
                    onDeleteSession = { viewModel.deleteSessionFile(it) },
                )

                is AppState.LoadingSession -> LoadingSessionScreen(
                    fileName = currentState.fileName,
                    scanDurationMillis = currentState.scanDurationMillis,
                )

                is AppState.Scanning -> ScanningScreen(
                    progress = currentState.progress,
                    source = currentState.source,
                    startTimeMillis = currentState.startTimeMillis,
                    onCancel = { viewModel.cancelScan() },
                )

                is AppState.Browsing -> BrowsingScreen(
                    currentNode = currentState.currentNode,
                    pathStack = currentState.pathStack,
                    sortMode = currentState.sortMode,
                    totalFiles = currentState.totalFiles,
                    totalDirectories = currentState.totalDirectories,
                    totalBytes = currentState.totalBytes,
                    scanDurationMillis = currentState.scanDurationMillis,
                    onNavigateInto = { viewModel.navigateInto(it) },
                    onNavigateUp = { viewModel.navigateUp() },
                    onNavigateTo = { viewModel.navigateTo(it) },
                    onToggleSort = { viewModel.toggleSort() },
                    onDelete = { viewModel.deleteNode(it) },
                    onOpen = { viewModel.openInFileBrowser(it) },
                    onSave = { viewModel.saveSession() },
                    onRestart = { viewModel.restart() },
                    estimatedSessionSize = viewModel.estimateSessionSize(),
                    loadedFromSession = currentState.loadedFromSession,
                    sessionSaved = currentState.sessionSaved,
                    saving = currentState.saving,
                )
            }

            // Auth prompt dialog
            authPrompt?.let { prompt ->
                AuthDialog(
                    prompt = prompt,
                    onSubmit = { viewModel.submitAuthResponse(it) },
                    onCancel = { viewModel.submitAuthResponse(null) },
                )
            }

            // Save success banner (auto-dismiss after 10s)
            saveMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(10_000)
                    viewModel.clearSaveMessage()
                }
                SuccessBanner(
                    message = msg,
                    onDismiss = { viewModel.clearSaveMessage() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                )
            }

            // Error snackbar
            error?.let { errorMsg ->
                ErrorBanner(
                    message = errorMsg,
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun AuthDialog(
    prompt: AuthPrompt,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FluentTheme.colors.background.solid.secondary)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (prompt.type == page.matthewt.diskspaceinvestigator.viewmodel.AuthPromptType.PASSPHRASE)
                    "SSH Key Passphrase" else "SSH Password",
                style = FluentTheme.typography.subtitle,
                fontWeight = FontWeight.Bold,
            )

            Text(prompt.message, color = FluentTheme.colors.text.text.secondary)

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { androidx.compose.material3.Text("Enter here...") },
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { onSubmit(input) }) { Text("Submit") }
            }
        }
    }
}

@Composable
private fun LoadingSessionScreen(fileName: String, scanDurationMillis: Long = 0) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        com.konyaco.fluent.component.ProgressRing(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "Loading session...",
            style = FluentTheme.typography.subtitle,
            color = FluentTheme.colors.text.text.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            fileName,
            color = FluentTheme.colors.text.text.secondary,
        )
        if (scanDurationMillis > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Original scan took ${page.matthewt.diskspaceinvestigator.ui.components.SizeDisplay.formatDuration(scanDurationMillis)}",
                color = FluentTheme.colors.text.text.secondary,
            )
        }
    }
}

@Composable
private fun SuccessBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color(0xFF1B5E20))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            color = androidx.compose.ui.graphics.Color.White,
        )
        Button(onClick = onDismiss) {
            Text("OK")
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FluentTheme.colors.background.solid.secondary)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            color = FluentTheme.colors.text.text.primary,
        )
        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}
