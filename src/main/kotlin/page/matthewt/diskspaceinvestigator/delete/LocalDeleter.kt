package page.matthewt.diskspaceinvestigator.delete

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class LocalDeleter : FileDeleter {

    override suspend fun delete(path: String): Long = withContext(Dispatchers.IO) {
        val target = Path.of(path)
        if (!Files.exists(target)) return@withContext 0L

        var bytesFreed = 0L

        if (Files.isDirectory(target)) {
            Files.walkFileTree(target, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    bytesFreed += attrs.size()
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: java.io.IOException?): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
        } else {
            bytesFreed = Files.size(target)
            Files.delete(target)
        }

        bytesFreed
    }
}
