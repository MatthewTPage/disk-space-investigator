package page.matthewt.diskspaceinvestigator.delete

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.common.SftpConstants

class SshDeleter(private val sftpClient: SftpClient) : FileDeleter {

    override suspend fun delete(path: String): Long = withContext(Dispatchers.IO) {
        return@withContext deleteRecursive(path)
    }

    private fun deleteRecursive(path: String): Long {
        val attrs = sftpClient.stat(path)

        return if (attrs.type == SftpConstants.SSH_FILEXFER_TYPE_DIRECTORY) {
            var bytesFreed = 0L
            val handle = sftpClient.openDir(path)

            val entries = mutableListOf<SftpClient.DirEntry>()
            var chunk = sftpClient.readDir(handle)
            while (chunk != null) {
                entries.addAll(chunk)
                chunk = try {
                    sftpClient.readDir(handle)
                } catch (_: Exception) {
                    null
                }
            }
            sftpClient.close(handle)

            for (entry in entries) {
                val name = entry.filename
                if (name == "." || name == "..") continue
                val childPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                bytesFreed += deleteRecursive(childPath)
            }

            sftpClient.rmdir(path)
            bytesFreed
        } else {
            val size = attrs.size
            sftpClient.remove(path)
            size
        }
    }
}
