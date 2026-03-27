package page.matthewt.diskspaceinvestigator.model

data class ScanProgress(
    val filesScanned: Long = 0,
    val directoriesScanned: Long = 0,
    val bytesTotal: Long = 0,
    val currentPath: String = "",
    val phase: ScanPhase = ScanPhase.STARTING,
) {
    enum class ScanPhase {
        STARTING,
        CONNECTING,
        SCANNING,
        AGGREGATING,
        COMPLETE,
        CANCELLED,
        ERROR,
    }
}
