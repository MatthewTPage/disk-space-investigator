package page.matthewt.diskspaceinvestigator.delete

interface FileDeleter {
    /**
     * Permanently deletes the file or directory at the given path.
     * For directories, this is recursive.
     * @return total bytes freed
     */
    suspend fun delete(path: String): Long
}
