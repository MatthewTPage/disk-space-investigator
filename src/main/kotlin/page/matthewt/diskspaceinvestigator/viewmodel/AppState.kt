package page.matthewt.diskspaceinvestigator.viewmodel

import page.matthewt.diskspaceinvestigator.model.*
import page.matthewt.diskspaceinvestigator.session.SessionInfo

sealed class AppState {
    data class Start(
        val sessions: List<SessionInfo> = emptyList(),
        val error: String? = null,
    ) : AppState()

    data class Scanning(
        val progress: ScanProgress = ScanProgress(),
        val source: ScanSource,
        val startTimeMillis: Long = System.currentTimeMillis(),
    ) : AppState()

    data class LoadingSession(
        val fileName: String,
        val scanDurationMillis: Long = 0,
    ) : AppState()

    data class Browsing(
        val rootNode: FileNode,
        val currentNode: FileNode,
        val pathStack: List<FileNode> = emptyList(),
        val source: ScanSource,
        val sortMode: SortMode = SortMode.SIZE_DESC,
        val totalFiles: Long = 0,
        val totalDirectories: Long = 0,
        val totalBytes: Long = 0,
        val inaccessibleCount: Long = 0,
        val symlinkCount: Long = 0,
        val scanDurationMillis: Long = 0,
        val loadedFromSession: Boolean = false,
        val sessionSaved: Boolean = false,
        val saving: Boolean = false,
    ) : AppState()
}

data class AuthPrompt(
    val type: AuthPromptType,
    val message: String,
)

enum class AuthPromptType {
    PASSPHRASE,
    PASSWORD,
}
