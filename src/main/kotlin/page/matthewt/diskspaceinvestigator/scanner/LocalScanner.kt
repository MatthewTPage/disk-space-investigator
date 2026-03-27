package page.matthewt.diskspaceinvestigator.scanner

import page.matthewt.diskspaceinvestigator.model.FileNode
import page.matthewt.diskspaceinvestigator.model.ScanProgress
import page.matthewt.diskspaceinvestigator.model.ScanProgress.ScanPhase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class LocalScanner(
    private val rootPath: String,
    private val scope: CoroutineScope,
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

    override suspend fun scan(): FileNode = withContext(Dispatchers.IO) {
        cancelled = false
        _progress.value = ScanProgress(phase = ScanPhase.SCANNING)

        val root = Path.of(rootPath)
        if (!Files.exists(root)) {
            throw IllegalArgumentException("Path does not exist: $rootPath")
        }
        if (!Files.isDirectory(root)) {
            throw IllegalArgumentException("Path is not a directory: $rootPath")
        }

        val rootNode = FileNode(
            name = root.fileName?.toString() ?: root.toString(),
            absolutePath = root.toAbsolutePath().toString(),
            sizeBytes = 0,
            isDirectory = true,
        )

        // Map from path to node for building tree
        val nodeMap = mutableMapOf<Path, FileNode>()
        nodeMap[root.toAbsolutePath().normalize()] = rootNode

        Files.walkFileTree(root, object : FileVisitor<Path> {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (cancelled) return FileVisitResult.TERMINATE

                val absDir = dir.toAbsolutePath().normalize()
                if (absDir != root.toAbsolutePath().normalize()) {
                    val node = FileNode(
                        name = dir.fileName?.toString() ?: dir.toString(),
                        absolutePath = absDir.toString(),
                        sizeBytes = 0,
                        isDirectory = true,
                    )
                    val parentPath = absDir.parent
                    val parentNode = nodeMap[parentPath]
                    if (parentNode != null) {
                        parentNode.children.add(node)
                        node.parent = parentNode
                    }
                    nodeMap[absDir] = node
                    directoriesScanned++
                }

                updateProgress(absDir.toString())
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (cancelled) return FileVisitResult.TERMINATE

                val absFile = file.toAbsolutePath().normalize()
                val isSymlink = attrs.isSymbolicLink
                val size = if (isSymlink) 0L else attrs.size()

                if (isSymlink) symlinkCount++

                val node = FileNode(
                    name = file.fileName.toString(),
                    absolutePath = absFile.toString(),
                    sizeBytes = size,
                    isDirectory = false,
                    isSymlink = isSymlink,
                )

                val parentPath = absFile.parent
                val parentNode = nodeMap[parentPath]
                if (parentNode != null) {
                    parentNode.children.add(node)
                    node.parent = parentNode
                }

                filesScanned++
                bytesTotal += size

                if (filesScanned % 500 == 0L) {
                    updateProgress(absFile.toString())
                }

                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                if (cancelled) return FileVisitResult.TERMINATE
                inaccessibleCount++
                val absFile = file.toAbsolutePath().normalize()

                // Still add the node so it shows up, marked as access denied
                val node = FileNode(
                    name = file.fileName?.toString() ?: file.toString(),
                    absolutePath = absFile.toString(),
                    sizeBytes = 0,
                    isDirectory = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS),
                    isAccessDenied = true,
                )
                val parentPath = absFile.parent
                val parentNode = nodeMap[parentPath]
                if (parentNode != null) {
                    parentNode.children.add(node)
                    node.parent = parentNode
                }
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (cancelled) return FileVisitResult.TERMINATE
                return FileVisitResult.CONTINUE
            }
        })

        if (cancelled) {
            _progress.value = ScanProgress(phase = ScanPhase.CANCELLED)
            throw CancellationException("Scan cancelled")
        }

        _progress.value = ScanProgress(
            filesScanned = filesScanned,
            directoriesScanned = directoriesScanned,
            bytesTotal = bytesTotal,
            currentPath = "",
            phase = ScanPhase.COMPLETE,
        )

        rootNode
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

    fun getScanResult(root: FileNode, elapsedMillis: Long) = ScanResult(
        root = root,
        totalFiles = filesScanned,
        totalDirectories = directoriesScanned,
        totalBytes = bytesTotal,
        inaccessibleCount = inaccessibleCount,
        symlinkCount = symlinkCount,
        elapsedMillis = elapsedMillis,
    )
}
