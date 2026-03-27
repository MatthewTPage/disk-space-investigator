package page.matthewt.diskspaceinvestigator.session

import page.matthewt.diskspaceinvestigator.model.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@OptIn(ExperimentalSerializationApi::class)
object SessionStore {

    private val cbor = Cbor {
        ignoreUnknownKeys = true
    }

    fun save(session: Session, file: File, onProgress: ((String) -> Unit)? = null) {
        file.parentFile?.mkdirs()
        onProgress?.invoke("Serializing tree data...")
        val bytes = cbor.encodeToByteArray(session)
        onProgress?.invoke("Compressing & writing to disk... (0%)")
        FileOutputStream(file).use { fos ->
            GZIPOutputStream(fos).use { gzip ->
                val chunkSize = 1024 * 1024 // 1MB chunks
                var written = 0
                while (written < bytes.size) {
                    val end = minOf(written + chunkSize, bytes.size)
                    gzip.write(bytes, written, end - written)
                    written = end
                    val pct = (written * 100L / bytes.size).toInt()
                    onProgress?.invoke("Compressing & writing to disk... ($pct%)")
                }
            }
        }
    }

    fun load(file: File): Session {
        FileInputStream(file).use { fis ->
            GZIPInputStream(fis).use { gzip ->
                val bytes = gzip.readBytes()
                val session = cbor.decodeFromByteArray<Session>(bytes)
                session.rootNode.relinkParents()
                return session
            }
        }
    }

    fun estimateSize(session: Session): Long {
        val bytes = cbor.encodeToByteArray(session)
        // Gzip typically achieves ~60-70% compression on tree data with repetitive paths
        return (bytes.size * 0.35).toLong()
    }

    fun exactSize(session: Session): Long {
        val bytes = cbor.encodeToByteArray(session)
        val compressed = java.io.ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(bytes)
            }
            baos.toByteArray()
        }
        return compressed.size.toLong()
    }
}
