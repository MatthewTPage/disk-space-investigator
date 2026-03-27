package page.matthewt.diskspaceinvestigator.scanner

import page.matthewt.diskspaceinvestigator.model.FileNode
import page.matthewt.diskspaceinvestigator.model.ScanProgress
import page.matthewt.diskspaceinvestigator.model.ScanProgress.ScanPhase
import page.matthewt.diskspaceinvestigator.model.ScanSource
import page.matthewt.diskspaceinvestigator.ssh.SshConnectionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.common.SftpConstants

class SshScanner(
    private val source: ScanSource.Ssh,
    private val connectionManager: SshConnectionManager,
    private val authCallbacks: SshConnectionManager.AuthCallbacks,
) : Scanner {

    private val _progress = MutableStateFlow(ScanProgress())
    override val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    @Volatile
    private var cancelled = false
    private var filesScanned = 0L
    private var directoriesScanned = 0L
    private var bytesTotal = 0L
    private var inaccessibleCount = 0L
    private var symlinkCount = 0L

    private var sftpClient: SftpClient? = null

    override suspend fun scan(): FileNode = withContext(Dispatchers.IO) {
        cancelled = false
        _progress.value = ScanProgress(phase = ScanPhase.CONNECTING)

        val sftp = connectionManager.connect(source, authCallbacks)
        sftpClient = sftp

        _progress.value = ScanProgress(phase = ScanPhase.SCANNING)

        val rootNode = scanDirectory(sftp, source.path, null)

        if (cancelled) {
            _progress.value = ScanProgress(phase = ScanPhase.CANCELLED)
            throw CancellationException("Scan cancelled")
        }

        _progress.value = ScanProgress(
            filesScanned = filesScanned,
            directoriesScanned = directoriesScanned,
            bytesTotal = bytesTotal,
            phase = ScanPhase.COMPLETE,
        )

        rootNode
    }

    private fun scanDirectory(sftp: SftpClient, path: String, parentNode: FileNode?): FileNode {
        if (cancelled) throw CancellationException("Scan cancelled")

        val dirName = path.substringAfterLast('/').ifEmpty { path }
        val dirNode = FileNode(
            name = dirName,
            absolutePath = path,
            sizeBytes = 0,
            isDirectory = true,
        )
        dirNode.parent = parentNode
        directoriesScanned++

        updateProgress(path)

        try {
            val handle = sftp.openDir(path)
            val entries = mutableListOf<SftpClient.DirEntry>()

            var chunk = sftp.readDir(handle)
            while (chunk != null) {
                entries.addAll(chunk)
                chunk = try {
                    sftp.readDir(handle)
                } catch (_: Exception) {
                    null
                }
            }
            sftp.close(handle)

            for (entry in entries) {
                if (cancelled) break
                val name = entry.filename
                if (name == "." || name == "..") continue

                val childPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                val attrs = entry.attributes

                val isSymlink = attrs.type == SftpConstants.SSH_FILEXFER_TYPE_SYMLINK
                val isDir = attrs.type == SftpConstants.SSH_FILEXFER_TYPE_DIRECTORY

                if (isSymlink) {
                    symlinkCount++
                    val symlinkNode = FileNode(
                        name = name,
                        absolutePath = childPath,
                        sizeBytes = 0,
                        isDirectory = false,
                        isSymlink = true,
                    )
                    symlinkNode.parent = dirNode
                    dirNode.children.add(symlinkNode)
                } else if (isDir) {
                    val childDir = scanDirectory(sftp, childPath, dirNode)
                    dirNode.children.add(childDir)
                } else {
                    val size = attrs.size
                    val fileNode = FileNode(
                        name = name,
                        absolutePath = childPath,
                        sizeBytes = size,
                        isDirectory = false,
                    )
                    fileNode.parent = dirNode
                    dirNode.children.add(fileNode)
                    filesScanned++
                    bytesTotal += size

                    if (filesScanned % 200 == 0L) {
                        updateProgress(childPath)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            inaccessibleCount++
            dirNode.children.add(
                FileNode(
                    name = dirNode.name,
                    absolutePath = path,
                    sizeBytes = 0,
                    isDirectory = true,
                    isAccessDenied = true,
                )
            )
        }

        return dirNode
    }

    override fun cancel() {
        cancelled = true
    }

    private fun updateProgress(currentPath: String) {
        _progress.value = ScanProgress(
            filesScanned = filesScanned,
            directoriesScanned = directoriesScanned,
            bytesTotal = bytesTotal,
            currentPath = currentPath,
            phase = ScanPhase.SCANNING,
        )
    }
}
