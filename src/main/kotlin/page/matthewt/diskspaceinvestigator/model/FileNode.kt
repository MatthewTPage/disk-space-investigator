package page.matthewt.diskspaceinvestigator.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FileNode(
    val name: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val children: MutableList<FileNode> = mutableListOf(),
    val isSymlink: Boolean = false,
    val isAccessDenied: Boolean = false,
) {
    @Transient
    var parent: FileNode? = null

    // Cached aggregates — computed once via computeAggregates(), invalidated on removal
    @Transient
    private var _cachedTotalSize: Long = -1L
    @Transient
    private var _cachedFileCount: Long = -1L
    @Transient
    private var _cachedDirectoryCount: Long = -1L

    val totalSize: Long
        get() {
            if (_cachedTotalSize >= 0) return _cachedTotalSize
            return if (isDirectory) {
                children.sumOf { it.totalSize }
            } else {
                sizeBytes
            }
        }

    val fileCount: Long
        get() {
            if (_cachedFileCount >= 0) return _cachedFileCount
            return if (isDirectory) {
                children.sumOf { it.fileCount }
            } else {
                1L
            }
        }

    val directoryCount: Long
        get() {
            if (_cachedDirectoryCount >= 0) return _cachedDirectoryCount
            return if (isDirectory) {
                1L + children.sumOf { it.directoryCount }
            } else {
                0L
            }
        }

    /**
     * Pre-computes and caches totalSize, fileCount, and directoryCount
     * for this node and all descendants. Call once after building/loading the tree.
     */
    fun computeAggregates() {
        if (!isDirectory) {
            _cachedTotalSize = sizeBytes
            _cachedFileCount = 1L
            _cachedDirectoryCount = 0L
            return
        }

        var size = 0L
        var files = 0L
        var dirs = 1L // count self

        for (child in children) {
            child.computeAggregates()
            size += child._cachedTotalSize
            files += child._cachedFileCount
            dirs += child._cachedDirectoryCount
        }

        _cachedTotalSize = size
        _cachedFileCount = files
        _cachedDirectoryCount = dirs
    }

    /**
     * Invalidates cached aggregates up to the root. Call after mutations (e.g. removeChild).
     */
    private fun invalidateCaches() {
        _cachedTotalSize = -1L
        _cachedFileCount = -1L
        _cachedDirectoryCount = -1L
        parent?.invalidateCaches()
    }

    fun sortedBySize(): List<FileNode> =
        children.sortedByDescending { it.totalSize }

    fun sortedByName(): List<FileNode> =
        children.sortedBy { it.name.lowercase() }

    fun sortedBy(mode: SortMode): List<FileNode> = when (mode) {
        SortMode.SIZE_DESC -> sortedBySize()
        SortMode.NAME_ASC -> sortedByName()
    }

    /**
     * Relinks parent references and computes cached aggregates after deserialization.
     */
    fun relinkParents() {
        for (child in children) {
            child.parent = this
            child.relinkParents()
        }
    }

    /**
     * Removes a child node and invalidates caches up the tree.
     */
    fun removeChild(child: FileNode): Boolean {
        val removed = children.remove(child)
        if (removed) {
            child.parent = null
            invalidateCaches()
        }
        return removed
    }
}
