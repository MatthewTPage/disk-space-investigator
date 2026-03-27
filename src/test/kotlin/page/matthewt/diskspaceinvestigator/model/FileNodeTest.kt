package page.matthewt.diskspaceinvestigator.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FileNodeTest {

    @Test
    fun `file node returns its own size as totalSize`() {
        val file = FileNode("test.txt", "/test.txt", 1024, isDirectory = false)
        assertEquals(1024L, file.totalSize)
    }

    @Test
    fun `directory aggregates children sizes`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("a.txt", "/dir/a.txt", 100, isDirectory = false),
            FileNode("b.txt", "/dir/b.txt", 200, isDirectory = false),
            FileNode("c.txt", "/dir/c.txt", 300, isDirectory = false),
        ))
        assertEquals(600L, dir.totalSize)
    }

    @Test
    fun `nested directory aggregates recursively`() {
        val inner = FileNode("inner", "/dir/inner", 0, isDirectory = true, children = mutableListOf(
            FileNode("deep.txt", "/dir/inner/deep.txt", 500, isDirectory = false),
        ))
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("top.txt", "/dir/top.txt", 100, isDirectory = false),
            inner,
        ))
        assertEquals(600L, dir.totalSize)
        assertEquals(500L, inner.totalSize)
    }

    @Test
    fun `empty directory has zero totalSize`() {
        val dir = FileNode("empty", "/empty", 0, isDirectory = true)
        assertEquals(0L, dir.totalSize)
    }

    @Test
    fun `fileCount counts only files`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("a.txt", "/dir/a.txt", 100, isDirectory = false),
            FileNode("sub", "/dir/sub", 0, isDirectory = true, children = mutableListOf(
                FileNode("b.txt", "/dir/sub/b.txt", 200, isDirectory = false),
                FileNode("c.txt", "/dir/sub/c.txt", 300, isDirectory = false),
            )),
        ))
        assertEquals(3L, dir.fileCount)
    }

    @Test
    fun `directoryCount includes self and nested dirs`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("sub1", "/dir/sub1", 0, isDirectory = true, children = mutableListOf(
                FileNode("sub2", "/dir/sub1/sub2", 0, isDirectory = true),
            )),
        ))
        // dir(1) + sub1(1) + sub2(1) = 3
        assertEquals(3L, dir.directoryCount)
    }

    @Test
    fun `sortedBySize returns largest first`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("small.txt", "/dir/small.txt", 10, isDirectory = false),
            FileNode("big.txt", "/dir/big.txt", 1000, isDirectory = false),
            FileNode("medium.txt", "/dir/medium.txt", 100, isDirectory = false),
        ))
        val sorted = dir.sortedBySize()
        assertEquals("big.txt", sorted[0].name)
        assertEquals("medium.txt", sorted[1].name)
        assertEquals("small.txt", sorted[2].name)
    }

    @Test
    fun `sortedByName returns alphabetical`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("Charlie.txt", "/dir/Charlie.txt", 10, isDirectory = false),
            FileNode("alpha.txt", "/dir/alpha.txt", 100, isDirectory = false),
            FileNode("Bravo.txt", "/dir/Bravo.txt", 50, isDirectory = false),
        ))
        val sorted = dir.sortedByName()
        assertEquals("alpha.txt", sorted[0].name)
        assertEquals("Bravo.txt", sorted[1].name)
        assertEquals("Charlie.txt", sorted[2].name)
    }

    @Test
    fun `sortedBy delegates to correct method`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(
            FileNode("z.txt", "/dir/z.txt", 1, isDirectory = false),
            FileNode("a.txt", "/dir/a.txt", 100, isDirectory = false),
        ))
        val bySize = dir.sortedBy(SortMode.SIZE_DESC)
        assertEquals("a.txt", bySize[0].name)

        val byName = dir.sortedBy(SortMode.NAME_ASC)
        assertEquals("a.txt", byName[0].name)
    }

    @Test
    fun `relinkParents sets parent references`() {
        val child = FileNode("child.txt", "/dir/child.txt", 10, isDirectory = false)
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(child))

        assertNull(child.parent)
        dir.relinkParents()
        assertSame(dir, child.parent)
    }

    @Test
    fun `removeChild removes and unlinks parent`() {
        val child = FileNode("child.txt", "/dir/child.txt", 10, isDirectory = false)
        val dir = FileNode("dir", "/dir", 0, isDirectory = true, children = mutableListOf(child))
        child.parent = dir

        assertTrue(dir.removeChild(child))
        assertTrue(dir.children.isEmpty())
        assertNull(child.parent)
    }

    @Test
    fun `removeChild returns false for non-child`() {
        val dir = FileNode("dir", "/dir", 0, isDirectory = true)
        val stranger = FileNode("stranger.txt", "/stranger.txt", 10, isDirectory = false)
        assertFalse(dir.removeChild(stranger))
    }

    @Test
    fun `symlink node has zero totalSize`() {
        val symlink = FileNode("link", "/link", 0, isDirectory = false, isSymlink = true)
        assertEquals(0L, symlink.totalSize)
    }

    @Test
    fun `access denied node has zero totalSize`() {
        val denied = FileNode("denied", "/denied", 0, isDirectory = true, isAccessDenied = true)
        assertEquals(0L, denied.totalSize)
    }

    @Test
    fun `deeply nested tree aggregates correctly`() {
        fun buildDeep(depth: Int, basePath: String): FileNode {
            val file = FileNode("file.txt", "$basePath/file.txt", 1, isDirectory = false)
            return if (depth == 0) {
                FileNode("d0", basePath, 0, isDirectory = true, children = mutableListOf(file))
            } else {
                val child = buildDeep(depth - 1, "$basePath/d$depth")
                FileNode("d$depth", basePath, 0, isDirectory = true, children = mutableListOf(file, child))
            }
        }
        val tree = buildDeep(10, "/root")
        // 11 levels, each with a 1-byte file = 11 bytes total
        assertEquals(11L, tree.totalSize)
        assertEquals(11L, tree.fileCount)
    }
}
