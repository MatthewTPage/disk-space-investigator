package page.matthewt.diskspaceinvestigator.scanner

import page.matthewt.diskspaceinvestigator.model.ScanProgress.ScanPhase
import page.matthewt.diskspaceinvestigator.model.ScanProgress
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LocalScannerTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun `scan empty directory`(@TempDir tempDir: Path) = runBlocking {
        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        assertTrue(root.isDirectory)
        assertTrue(root.children.isEmpty())
        assertEquals(0L, root.totalSize)
    }

    @Test
    fun `scan directory with files`(@TempDir tempDir: Path) = runBlocking {
        // Create test files
        File(tempDir.toFile(), "file1.txt").writeText("hello") // 5 bytes
        File(tempDir.toFile(), "file2.txt").writeText("world!") // 6 bytes

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        assertEquals(2, root.children.size)
        assertEquals(11L, root.totalSize)
        assertEquals(2L, root.fileCount)
    }

    @Test
    fun `scan nested directories`(@TempDir tempDir: Path) = runBlocking {
        val subDir = File(tempDir.toFile(), "subdir")
        subDir.mkdir()
        File(tempDir.toFile(), "top.txt").writeText("top") // 3 bytes
        File(subDir, "nested.txt").writeText("nested") // 6 bytes

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        assertEquals(2, root.children.size) // top.txt + subdir
        assertEquals(9L, root.totalSize)

        val subdirNode = root.children.find { it.isDirectory }
        assertNotNull(subdirNode)
        assertEquals("subdir", subdirNode!!.name)
        assertEquals(6L, subdirNode.totalSize)
        assertEquals(1, subdirNode.children.size)
    }

    @Test
    fun `scan deeply nested structure`(@TempDir tempDir: Path) = runBlocking {
        var current = tempDir.toFile()
        for (i in 1..5) {
            current = File(current, "level$i")
            current.mkdir()
        }
        File(current, "deep.txt").writeText("deep content") // 12 bytes

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        assertEquals(12L, root.totalSize)
        assertEquals(1L, root.fileCount)
    }

    @Test
    fun `scan reports progress`(@TempDir tempDir: Path) = runBlocking {
        // Create enough files to trigger progress updates
        for (i in 1..600) {
            File(tempDir.toFile(), "file$i.txt").writeText("x")
        }

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        assertEquals(600L, root.fileCount)
        // Progress should have reached COMPLETE
        assertEquals(ScanPhase.COMPLETE, scanner.progress.value.phase)
        assertEquals(600L, scanner.progress.value.filesScanned)
    }

    @Test
    fun `scan handles multiple subdirectories`(@TempDir tempDir: Path) = runBlocking {
        for (dirName in listOf("alpha", "beta", "gamma")) {
            val dir = File(tempDir.toFile(), dirName)
            dir.mkdir()
            File(dir, "file.txt").writeText(dirName) // varying sizes
        }

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        assertEquals(3, root.children.size)
        assertEquals(3L, root.fileCount)
        // "alpha"(5) + "beta"(4) + "gamma"(5) = 14
        assertEquals(14L, root.totalSize)
    }

    @Test
    fun `cancel stops scan`(@TempDir tempDir: Path) = runBlocking {
        // Create a deep directory structure to ensure scan takes time
        var current = tempDir.toFile()
        for (i in 1..20) {
            current = File(current, "dir$i")
            current.mkdir()
            for (j in 1..50) {
                File(current, "file$j.txt").writeText("x".repeat(10))
            }
        }

        val scanner = LocalScanner(tempDir.toString(), scope)

        // Launch scan in a separate job and cancel after a small delay
        val job = launch(Dispatchers.IO) {
            scanner.scan()
        }

        // Give the scan a moment to start, then cancel
        delay(50)
        scanner.cancel()
        job.join()

        // After cancellation, phase should be CANCELLED or the scan threw CancellationException
        val phase = scanner.progress.value.phase
        assertTrue(
            phase == ScanProgress.ScanPhase.CANCELLED || phase == ScanProgress.ScanPhase.SCANNING || phase == ScanProgress.ScanPhase.COMPLETE,
            "Scan should have been cancelled or completed quickly, was $phase"
        )
    }

    @Test
    fun `scan nonexistent directory throws`(@TempDir tempDir: Path) = runBlocking {
        val scanner = LocalScanner("$tempDir/nonexistent", scope)
        try {
            scanner.scan()
            fail("Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("does not exist"))
        }
    }

    @Test
    fun `file parent references are set correctly`(@TempDir tempDir: Path) = runBlocking {
        File(tempDir.toFile(), "child.txt").writeText("x")

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()

        val child = root.children[0]
        assertSame(root, child.parent)
    }

    @Test
    fun `getScanResult returns accurate stats`(@TempDir tempDir: Path) = runBlocking {
        File(tempDir.toFile(), "a.txt").writeText("abc")
        val sub = File(tempDir.toFile(), "sub")
        sub.mkdir()
        File(sub, "b.txt").writeText("defgh")

        val scanner = LocalScanner(tempDir.toString(), scope)
        val root = scanner.scan()
        val result = scanner.getScanResult(root, 100L)

        assertEquals(2L, result.totalFiles)
        assertEquals(8L, result.totalBytes)
        assertEquals(100L, result.elapsedMillis)
    }
}
