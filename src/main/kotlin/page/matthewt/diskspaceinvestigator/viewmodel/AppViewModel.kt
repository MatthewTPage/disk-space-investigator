package page.matthewt.diskspaceinvestigator.viewmodel

import page.matthewt.diskspaceinvestigator.delete.FileDeleter
import page.matthewt.diskspaceinvestigator.delete.LocalDeleter
import page.matthewt.diskspaceinvestigator.delete.SshDeleter
import page.matthewt.diskspaceinvestigator.model.*
import page.matthewt.diskspaceinvestigator.scanner.LocalScanner
import page.matthewt.diskspaceinvestigator.scanner.Scanner
import page.matthewt.diskspaceinvestigator.scanner.SshScanner
import page.matthewt.diskspaceinvestigator.session.SessionManager
import page.matthewt.diskspaceinvestigator.session.SessionStore
import page.matthewt.diskspaceinvestigator.ssh.SshConnectionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.Desktop
import java.io.File

class AppViewModel(
    private val scope: CoroutineScope,
    private val sessionManager: SessionManager = SessionManager(),
) {
    private val _state = MutableStateFlow<AppState>(AppState.Start())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _authPrompt = MutableStateFlow<AuthPrompt?>(null)
    val authPrompt: StateFlow<AuthPrompt?> = _authPrompt.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentScanner: Scanner? = null
    private var scanJob: Job? = null
    private var sshConnectionManager: SshConnectionManager? = null
    private var sftpClient: org.apache.sshd.sftp.client.SftpClient? = null

    // For passphrase/password dialog completion
    private var authContinuation: CompletableDeferred<String?>? = null

    init {
        loadStartScreen()
    }

    fun loadStartScreen() {
        val sessions = sessionManager.listSessions()
        _state.value = AppState.Start(sessions = sessions)
    }

    fun startLocalScan(path: String) {
        val source = ScanSource.Local(path)
        startScan(source)
    }

    fun startSshScan(host: String, user: String, path: String, port: Int = 22) {
        val source = ScanSource.Ssh(host = host, user = user, path = path, port = port)
        startScan(source)
    }

    private fun startScan(source: ScanSource) {
        scanJob?.cancel()

        _state.value = AppState.Scanning(source = source)

        scanJob = scope.launch {
            try {
                val scanner: Scanner = when (source) {
                    is ScanSource.Local -> LocalScanner(source.path, scope)
                    is ScanSource.Ssh -> {
                        val connMgr = SshConnectionManager()
                        sshConnectionManager = connMgr
                        SshScanner(source, connMgr, SshConnectionManager.AuthCallbacks(
                            onPassphraseNeeded = { keyFile ->
                                _authPrompt.value = AuthPrompt(
                                    AuthPromptType.PASSPHRASE,
                                    "Enter passphrase for key '$keyFile':"
                                )
                                val deferred = CompletableDeferred<String?>()
                                authContinuation = deferred
                                deferred.await()
                            },
                            onPasswordNeeded = { host ->
                                _authPrompt.value = AuthPrompt(
                                    AuthPromptType.PASSWORD,
                                    "Enter password for '$host':"
                                )
                                val deferred = CompletableDeferred<String?>()
                                authContinuation = deferred
                                deferred.await()
                            },
                        ))
                    }
                }

                currentScanner = scanner

                // Collect progress updates
                launch {
                    scanner.progress.collect { progress ->
                        val currentState = _state.value
                        if (currentState is AppState.Scanning) {
                            _state.value = currentState.copy(progress = progress)
                        }
                    }
                }

                val startTime = System.currentTimeMillis()
                val rootNode = scanner.scan()
                rootNode.computeAggregates()
                val elapsed = System.currentTimeMillis() - startTime

                val totalFiles = rootNode.fileCount
                val totalDirs = rootNode.directoryCount
                val totalBytes = rootNode.totalSize

                _state.value = AppState.Browsing(
                    rootNode = rootNode,
                    currentNode = rootNode,
                    pathStack = listOf(rootNode),
                    source = source,
                    totalFiles = totalFiles,
                    totalDirectories = totalDirs,
                    totalBytes = totalBytes,
                    scanDurationMillis = elapsed,
                )
            } catch (e: CancellationException) {
                loadStartScreen()
            } catch (e: Exception) {
                _state.value = AppState.Start(
                    sessions = sessionManager.listSessions(),
                    error = "Scan failed: ${e.message}"
                )
            }
        }
    }

    fun submitAuthResponse(response: String?) {
        _authPrompt.value = null
        authContinuation?.complete(response)
        authContinuation = null
    }

    fun cancelScan() {
        currentScanner?.cancel()
        scanJob?.cancel()
        sshConnectionManager?.disconnect()
        sshConnectionManager = null
        loadStartScreen()
    }

    fun navigateInto(node: FileNode) {
        val currentState = _state.value
        if (currentState is AppState.Browsing && node.isDirectory) {
            _state.value = currentState.copy(
                currentNode = node,
                pathStack = currentState.pathStack + node,
            )
        }
    }

    fun navigateUp() {
        val currentState = _state.value
        if (currentState is AppState.Browsing && currentState.pathStack.size > 1) {
            val newStack = currentState.pathStack.dropLast(1)
            _state.value = currentState.copy(
                currentNode = newStack.last(),
                pathStack = newStack,
            )
        }
    }

    fun navigateTo(index: Int) {
        val currentState = _state.value
        if (currentState is AppState.Browsing && index < currentState.pathStack.size) {
            val newStack = currentState.pathStack.take(index + 1)
            _state.value = currentState.copy(
                currentNode = newStack.last(),
                pathStack = newStack,
            )
        }
    }

    fun toggleSort() {
        val currentState = _state.value
        if (currentState is AppState.Browsing) {
            val newSort = when (currentState.sortMode) {
                SortMode.SIZE_DESC -> SortMode.NAME_ASC
                SortMode.NAME_ASC -> SortMode.SIZE_DESC
            }
            _state.value = currentState.copy(sortMode = newSort)
        }
    }

    fun deleteNode(node: FileNode) {
        val currentState = _state.value
        if (currentState !is AppState.Browsing) return

        _state.value = currentState.copy(
            deletingPaths = currentState.deletingPaths + node.absolutePath,
        )

        scope.launch {
            try {
                val deleter: FileDeleter = when (currentState.source) {
                    is ScanSource.Local -> LocalDeleter()
                    is ScanSource.Ssh -> {
                        // Need an SFTP client for SSH deletion
                        // For now, reuse connection or reconnect
                        throw UnsupportedOperationException("SSH delete requires active connection")
                    }
                }

                val bytesFreed = deleter.delete(node.absolutePath)

                // Remove node from parent
                node.parent?.removeChild(node)

                // Refresh state
                val latestState = _state.value
                if (latestState is AppState.Browsing) {
                    _state.value = latestState.copy(
                        totalBytes = latestState.totalBytes - bytesFreed,
                        totalFiles = latestState.totalFiles - node.fileCount,
                        totalDirectories = latestState.totalDirectories - node.directoryCount,
                        deletingPaths = latestState.deletingPaths - node.absolutePath,
                    )
                }
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
                val latestState = _state.value
                if (latestState is AppState.Browsing) {
                    _state.value = latestState.copy(
                        deletingPaths = latestState.deletingPaths - node.absolutePath,
                    )
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    fun saveSession() {
        val currentState = _state.value
        if (currentState !is AppState.Browsing) return
        if (currentState.saving || currentState.sessionSaved) return

        _state.value = currentState.copy(saving = true, saveProgress = "Preparing...")

        scope.launch {
            try {
                val session = Session(
                    source = currentState.source,
                    rootNode = currentState.rootNode,
                    timestampMillis = System.currentTimeMillis(),
                    totalFiles = currentState.totalFiles,
                    totalDirectories = currentState.totalDirectories,
                    totalBytes = currentState.totalBytes,
                    inaccessibleCount = currentState.inaccessibleCount,
                    symlinkCount = currentState.symlinkCount,
                    scanDurationMillis = currentState.scanDurationMillis,
                )

                val file = withContext(Dispatchers.IO) {
                    sessionManager.saveSession(session) { progress ->
                        val s = _state.value
                        if (s is AppState.Browsing) {
                            _state.value = s.copy(saveProgress = progress)
                        }
                    }
                }
                val sizeStr = page.matthewt.diskspaceinvestigator.ui.components.SizeDisplay.format(file.length())
                _saveMessage.value = "Saved to ${file.absolutePath} ($sizeStr)"

                val latestState = _state.value
                if (latestState is AppState.Browsing) {
                    _state.value = latestState.copy(saving = false, sessionSaved = true)
                }
            } catch (e: Exception) {
                _error.value = "Save failed: ${e.message}"
                val latestState = _state.value
                if (latestState is AppState.Browsing) {
                    _state.value = latestState.copy(saving = false)
                }
            }
        }
    }

    fun estimateSessionSize(): Long {
        val currentState = _state.value
        if (currentState !is AppState.Browsing) return 0L

        val session = Session(
            source = currentState.source,
            rootNode = currentState.rootNode,
            timestampMillis = System.currentTimeMillis(),
            totalFiles = currentState.totalFiles,
            totalDirectories = currentState.totalDirectories,
            totalBytes = currentState.totalBytes,
        )

        return SessionStore.estimateSize(session)
    }

    fun loadSession(file: File, scanDurationMillis: Long = 0) {
        _state.value = AppState.LoadingSession(fileName = file.name, scanDurationMillis = scanDurationMillis)

        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) {
                    val s = sessionManager.loadSession(file)
                    s.rootNode.computeAggregates()
                    s
                }
                _state.value = AppState.Browsing(
                    rootNode = session.rootNode,
                    currentNode = session.rootNode,
                    pathStack = listOf(session.rootNode),
                    source = session.source,
                    totalFiles = session.totalFiles,
                    totalDirectories = session.totalDirectories,
                    totalBytes = session.totalBytes,
                    inaccessibleCount = session.inaccessibleCount,
                    symlinkCount = session.symlinkCount,
                    scanDurationMillis = session.scanDurationMillis,
                    loadedFromSession = true,
                )
            } catch (e: Exception) {
                _state.value = AppState.Start(
                    sessions = sessionManager.listSessions(),
                    error = "Failed to load session: ${e.message}"
                )
            }
        }
    }

    fun deleteSessionFile(file: File) {
        sessionManager.deleteSession(file)
        loadStartScreen()
    }

    fun restart() {
        scanJob?.cancel()
        currentScanner = null
        sshConnectionManager?.disconnect()
        sshConnectionManager = null
        loadStartScreen()
    }

    fun openInFileBrowser(node: FileNode) {
        try {
            val file = File(node.absolutePath)
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (node.isDirectory) {
                    desktop.open(file)
                } else {
                    // Open parent directory with file selected (best effort)
                    desktop.open(file.parentFile ?: file)
                }
            }
        } catch (e: Exception) {
            _error.value = "Could not open file browser: ${e.message}"
        }
    }
}
