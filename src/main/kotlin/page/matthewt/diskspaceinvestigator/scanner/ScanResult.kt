package page.matthewt.diskspaceinvestigator.scanner

import page.matthewt.diskspaceinvestigator.model.FileNode

data class ScanResult(
    val root: FileNode,
    val totalFiles: Long,
    val totalDirectories: Long,
    val totalBytes: Long,
    val inaccessibleCount: Long,
    val symlinkCount: Long,
    val elapsedMillis: Long,
)
