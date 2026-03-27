package page.matthewt.diskspaceinvestigator.session

import page.matthewt.diskspaceinvestigator.model.Session
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class SessionInfo(
    val file: File,
    val displayName: String,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val scannedSizeBytes: Long = 0,
    val scanDurationMillis: Long = 0,
)

class SessionManager {

    private val sessionsDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".disk-space-investigator/sessions")
        dir.mkdirs()
        dir
    }

    fun listSessions(): List<SessionInfo> {
        if (!sessionsDir.exists()) return emptyList()

        return sessionsDir.listFiles { f -> f.extension == "dsi" }
            ?.map { file ->
                val metaFile = File(file.parentFile, "${file.nameWithoutExtension}.meta")
                val metaLines = if (metaFile.exists()) metaFile.readLines() else emptyList()
                val displayName = metaLines.getOrNull(0)?.trim() ?: file.nameWithoutExtension
                val scannedSize = metaLines.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                val scanDuration = metaLines.getOrNull(2)?.trim()?.toLongOrNull() ?: 0L
                SessionInfo(
                    file = file,
                    displayName = displayName,
                    fileSizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    scannedSizeBytes = scannedSize,
                    scanDurationMillis = scanDuration,
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    fun saveSession(session: Session, onProgress: ((String) -> Unit)? = null): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date(session.timestampMillis))
        val safeName = session.source.displayName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(50)
        val fileName = "${safeName}_$timestamp"
        val file = File(sessionsDir, "$fileName.dsi")
        SessionStore.save(session, file, onProgress)

        onProgress?.invoke("Saving metadata...")
        val metaFile = File(sessionsDir, "$fileName.meta")
        metaFile.writeText("${session.source.displayName}\n${session.totalBytes}\n${session.scanDurationMillis}")

        return file
    }

    fun loadSession(file: File): Session {
        return SessionStore.load(file)
    }

    fun deleteSession(file: File) {
        file.delete()
        // Also delete the metadata sidecar
        val metaFile = File(file.parentFile, "${file.nameWithoutExtension}.meta")
        metaFile.delete()
    }
}
