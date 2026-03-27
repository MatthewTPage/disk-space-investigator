package page.matthewt.diskspaceinvestigator.delete

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LocalDeleterTest {

    private val deleter = LocalDeleter()

    @Test
    fun `delete single file`(@TempDir tempDir: Path) = runBlocking {
        val file = File(tempDir.toFile(), "test.txt")
        file.writeText("hello world") // 11 bytes

        val bytesFreed = deleter.delete(file.absolutePath)

        assertFalse(file.exists())
        assertEquals(11L, bytesFreed)
    }

    @Test
    fun `delete empty directory`(@TempDir tempDir: Path) = runBlocking {
        val dir = File(tempDir.toFile(), "emptydir")
        dir.mkdir()

        val bytesFreed = deleter.delete(dir.absolutePath)

        assertFalse(dir.exists())
        assertEquals(0L, bytesFreed)
    }

    @Test
    fun `delete directory with files recursively`(@TempDir tempDir: Path) = runBlocking {
        val dir = File(tempDir.toFile(), "mydir")
        dir.mkdir()
        File(dir, "a.txt").writeText("aaa") // 3 bytes
        File(dir, "b.txt").writeText("bbbbb") // 5 bytes

        val bytesFreed = deleter.delete(dir.absolutePath)

        assertFalse(dir.exists())
        assertEquals(8L, bytesFreed)
    }

    @Test
    fun `delete nested directories recursively`(@TempDir tempDir: Path) = runBlocking {
        val dir = File(tempDir.toFile(), "outer")
        val inner = File(dir, "inner")
        inner.mkdirs()
        File(dir, "outer.txt").writeText("out") // 3 bytes
        File(inner, "inner.txt").writeText("in") // 2 bytes

        val bytesFreed = deleter.delete(dir.absolutePath)

        assertFalse(dir.exists())
        assertEquals(5L, bytesFreed)
    }

    @Test
    fun `delete nonexistent path returns zero`(@TempDir tempDir: Path) = runBlocking {
        val bytesFreed = deleter.delete("$tempDir/nonexistent")
        assertEquals(0L, bytesFreed)
    }

    @Test
    fun `delete does not affect sibling files`(@TempDir tempDir: Path) = runBlocking {
        val target = File(tempDir.toFile(), "target.txt")
        val sibling = File(tempDir.toFile(), "sibling.txt")
        target.writeText("target")
        sibling.writeText("sibling")

        deleter.delete(target.absolutePath)

        assertFalse(target.exists())
        assertTrue(sibling.exists())
        assertEquals("sibling", sibling.readText())
    }
}
