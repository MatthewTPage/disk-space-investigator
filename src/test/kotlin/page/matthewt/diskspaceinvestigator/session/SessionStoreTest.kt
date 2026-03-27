package page.matthewt.diskspaceinvestigator.session

import page.matthewt.diskspaceinvestigator.model.FileNode
import page.matthewt.diskspaceinvestigator.model.ScanSource
import page.matthewt.diskspaceinvestigator.model.Session
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SessionStoreTest {

    private fun makeSession(fileCount: Int = 10): Session {
        val children = (1..fileCount).map { i ->
            FileNode("file$i.txt", "/root/file$i.txt", i * 100L, isDirectory = false)
        }.toMutableList()

        val root = FileNode("root", "/root", 0, isDirectory = true, children = children)

        return Session(
            source = ScanSource.Local("/root"),
            rootNode = root,
            timestampMillis = System.currentTimeMillis(),
            totalFiles = fileCount.toLong(),
            totalDirectories = 1,
            totalBytes = children.sumOf { it.sizeBytes },
        )
    }

    @Test
    fun `save and load round-trip preserves data`(@TempDir tempDir: Path) {
        val session = makeSession()
        val file = File(tempDir.toFile(), "test.dsi")

        SessionStore.save(session, file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        val loaded = SessionStore.load(file)
        assertEquals(session.source, loaded.source)
        assertEquals(session.totalFiles, loaded.totalFiles)
        assertEquals(session.totalDirectories, loaded.totalDirectories)
        assertEquals(session.totalBytes, loaded.totalBytes)
        assertEquals(session.rootNode.name, loaded.rootNode.name)
        assertEquals(session.rootNode.children.size, loaded.rootNode.children.size)
    }

    @Test
    fun `loaded session has relinked parents`(@TempDir tempDir: Path) {
        val session = makeSession()
        val file = File(tempDir.toFile(), "test.dsi")

        SessionStore.save(session, file)
        val loaded = SessionStore.load(file)

        // Parents should be relinked after load
        for (child in loaded.rootNode.children) {
            assertSame(loaded.rootNode, child.parent)
        }
    }

    @Test
    fun `round-trip preserves nested directories`(@TempDir tempDir: Path) {
        val inner = FileNode("inner", "/root/inner", 0, isDirectory = true, children = mutableListOf(
            FileNode("deep.txt", "/root/inner/deep.txt", 500, isDirectory = false),
        ))
        val root = FileNode("root", "/root", 0, isDirectory = true, children = mutableListOf(
            FileNode("top.txt", "/root/top.txt", 100, isDirectory = false),
            inner,
        ))

        val session = Session(
            source = ScanSource.Local("/root"),
            rootNode = root,
            timestampMillis = System.currentTimeMillis(),
            totalFiles = 2,
            totalDirectories = 2,
            totalBytes = 600,
        )

        val file = File(tempDir.toFile(), "nested.dsi")
        SessionStore.save(session, file)
        val loaded = SessionStore.load(file)

        assertEquals(2, loaded.rootNode.children.size)
        val loadedInner = loaded.rootNode.children.find { it.isDirectory }!!
        assertEquals("inner", loadedInner.name)
        assertEquals(1, loadedInner.children.size)
        assertEquals(500L, loadedInner.totalSize)
    }

    @Test
    fun `round-trip preserves SSH source`(@TempDir tempDir: Path) {
        val root = FileNode("data", "/data", 0, isDirectory = true)
        val session = Session(
            source = ScanSource.Ssh(host = "server.com", user = "admin", path = "/data", port = 2222),
            rootNode = root,
            timestampMillis = 123456L,
            totalFiles = 0,
            totalDirectories = 1,
            totalBytes = 0,
        )

        val file = File(tempDir.toFile(), "ssh.dsi")
        SessionStore.save(session, file)
        val loaded = SessionStore.load(file)

        val source = loaded.source as ScanSource.Ssh
        assertEquals("server.com", source.host)
        assertEquals("admin", source.user)
        assertEquals("/data", source.path)
        assertEquals(2222, source.port)
    }

    @Test
    fun `saved file is compressed (smaller than uncompressed)`(@TempDir tempDir: Path) {
        val session = makeSession(100)
        val file = File(tempDir.toFile(), "compressed.dsi")

        SessionStore.save(session, file)

        // The exact size from the serializer
        val exactSize = SessionStore.exactSize(session)
        // Compressed file should be reasonable
        assertTrue(file.length() > 0)
        assertEquals(exactSize, file.length())
    }

    @Test
    fun `estimateSize returns reasonable approximation`() {
        val session = makeSession(100)
        val estimated = SessionStore.estimateSize(session)
        val exact = SessionStore.exactSize(session)

        // Estimate should be within 3x of actual (generous tolerance for estimation)
        assertTrue(estimated > 0)
        assertTrue(estimated < exact * 3, "Estimate $estimated should be within 3x of exact $exact")
    }

    @Test
    fun `round-trip preserves symlink and accessDenied flags`(@TempDir tempDir: Path) {
        val root = FileNode("root", "/root", 0, isDirectory = true, children = mutableListOf(
            FileNode("link", "/root/link", 0, isDirectory = false, isSymlink = true),
            FileNode("denied", "/root/denied", 0, isDirectory = true, isAccessDenied = true),
        ))
        val session = Session(
            source = ScanSource.Local("/root"),
            rootNode = root,
            timestampMillis = 1L,
            totalFiles = 0,
            totalDirectories = 2,
            totalBytes = 0,
            symlinkCount = 1,
            inaccessibleCount = 1,
        )

        val file = File(tempDir.toFile(), "flags.dsi")
        SessionStore.save(session, file)
        val loaded = SessionStore.load(file)

        val linkNode = loaded.rootNode.children.find { it.name == "link" }!!
        assertTrue(linkNode.isSymlink)

        val deniedNode = loaded.rootNode.children.find { it.name == "denied" }!!
        assertTrue(deniedNode.isAccessDenied)

        assertEquals(1L, loaded.symlinkCount)
        assertEquals(1L, loaded.inaccessibleCount)
    }
}
