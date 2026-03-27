package page.matthewt.diskspaceinvestigator.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val source: ScanSource,
    val rootNode: FileNode,
    val timestampMillis: Long,
    val totalFiles: Long,
    val totalDirectories: Long,
    val totalBytes: Long,
    val inaccessibleCount: Long = 0,
    val symlinkCount: Long = 0,
    val scanDurationMillis: Long = 0,
)
