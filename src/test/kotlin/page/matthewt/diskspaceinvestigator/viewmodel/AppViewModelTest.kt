package page.matthewt.diskspaceinvestigator.viewmodel

import page.matthewt.diskspaceinvestigator.model.FileNode
import page.matthewt.diskspaceinvestigator.model.ScanSource
import page.matthewt.diskspaceinvestigator.model.SortMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AppViewModelTest {

    private fun makeBrowsingState(): AppState.Browsing {
        val child1 = FileNode("big.txt", "/root/big.txt", 1000, isDirectory = false)
        val child2 = FileNode("small.txt", "/root/small.txt", 100, isDirectory = false)
        val subDir = FileNode("sub", "/root/sub", 0, isDirectory = true, children = mutableListOf(
            FileNode("inner.txt", "/root/sub/inner.txt", 500, isDirectory = false),
        ))
        val root = FileNode("root", "/root", 0, isDirectory = true, children = mutableListOf(
            child1, child2, subDir,
        ))
        root.relinkParents()

        return AppState.Browsing(
            rootNode = root,
            currentNode = root,
            pathStack = listOf(root),
            source = ScanSource.Local("/root"),
            totalFiles = 3,
            totalDirectories = 2,
            totalBytes = 1600,
        )
    }

    @Test
    fun `initial state is Start`() {
        val state = AppState.Start()
        assertTrue(state.sessions.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `browsing state defaults to SIZE_DESC sort`() {
        val state = makeBrowsingState()
        assertEquals(SortMode.SIZE_DESC, state.sortMode)
    }

    @Test
    fun `browsing currentNode sorts children by size`() {
        val state = makeBrowsingState()
        val sorted = state.currentNode.sortedBy(state.sortMode)
        assertEquals("big.txt", sorted[0].name)
        assertEquals("sub", sorted[1].name) // 500 bytes
        assertEquals("small.txt", sorted[2].name) // 100 bytes
    }

    @Test
    fun `browsing currentNode sorts children by name`() {
        val state = makeBrowsingState()
        val sorted = state.currentNode.sortedBy(SortMode.NAME_ASC)
        assertEquals("big.txt", sorted[0].name)
        assertEquals("small.txt", sorted[1].name)
        assertEquals("sub", sorted[2].name)
    }

    @Test
    fun `path stack starts with root`() {
        val state = makeBrowsingState()
        assertEquals(1, state.pathStack.size)
        assertSame(state.rootNode, state.pathStack[0])
    }

    @Test
    fun `navigating into subdirectory extends path stack`() {
        val state = makeBrowsingState()
        val sub = state.rootNode.children.find { it.isDirectory }!!
        val newState = state.copy(
            currentNode = sub,
            pathStack = state.pathStack + sub,
        )
        assertEquals(2, newState.pathStack.size)
        assertSame(sub, newState.currentNode)
    }

    @Test
    fun `navigating up pops path stack`() {
        val state = makeBrowsingState()
        val sub = state.rootNode.children.find { it.isDirectory }!!
        val navigated = state.copy(
            currentNode = sub,
            pathStack = state.pathStack + sub,
        )

        val newStack = navigated.pathStack.dropLast(1)
        val back = navigated.copy(
            currentNode = newStack.last(),
            pathStack = newStack,
        )

        assertEquals(1, back.pathStack.size)
        assertSame(state.rootNode, back.currentNode)
    }

    @Test
    fun `toggle sort switches modes`() {
        val state = makeBrowsingState()
        assertEquals(SortMode.SIZE_DESC, state.sortMode)

        val toggled = state.copy(sortMode = SortMode.NAME_ASC)
        assertEquals(SortMode.NAME_ASC, toggled.sortMode)

        val toggledBack = toggled.copy(sortMode = SortMode.SIZE_DESC)
        assertEquals(SortMode.SIZE_DESC, toggledBack.sortMode)
    }

    @Test
    fun `scanning state tracks source`() {
        val source = ScanSource.Ssh("myhost", "user", "/data", 22)
        val state = AppState.Scanning(source = source)
        assertEquals("user@myhost:/data", state.source.displayName)
    }

    @Test
    fun `local source display name is path`() {
        val source = ScanSource.Local("/home/user/Documents")
        assertEquals("/home/user/Documents", source.displayName)
    }

    @Test
    fun `ssh source display name includes user and host`() {
        val source = ScanSource.Ssh("server.com", "admin", "/var/log", 2222)
        assertEquals("admin@server.com:/var/log", source.displayName)
    }

    @Test
    fun `removing child updates parent totalSize`() {
        val state = makeBrowsingState()
        val root = state.rootNode
        val bigFile = root.children.find { it.name == "big.txt" }!!

        assertEquals(1600L, root.totalSize)
        root.removeChild(bigFile)
        assertEquals(600L, root.totalSize)
    }
}
