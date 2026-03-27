package page.matthewt.diskspaceinvestigator.scanner

import page.matthewt.diskspaceinvestigator.model.FileNode
import page.matthewt.diskspaceinvestigator.model.ScanProgress
import kotlinx.coroutines.flow.StateFlow

interface Scanner {
    val progress: StateFlow<ScanProgress>
    suspend fun scan(): FileNode
    fun cancel()
}
