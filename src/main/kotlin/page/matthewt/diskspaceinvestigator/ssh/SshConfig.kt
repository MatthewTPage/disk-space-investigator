package page.matthewt.diskspaceinvestigator.ssh

import java.io.File

data class SshHostEntry(
    val alias: String,
    val hostName: String,
    val user: String?,
    val port: Int?,
    val identityFile: String?,
)

object SshConfig {
    fun parseConfig(): List<SshHostEntry> {
        val configFile = File(System.getProperty("user.home"), ".ssh/config")
        if (!configFile.exists()) return emptyList()

        val entries = mutableListOf<SshHostEntry>()
        var currentAlias: String? = null
        var hostName: String? = null
        var user: String? = null
        var port: Int? = null
        var identityFile: String? = null

        fun flushEntry() {
            val alias = currentAlias ?: return
            if (alias.contains('*') || alias.contains('?')) return // skip wildcards
            entries.add(
                SshHostEntry(
                    alias = alias,
                    hostName = hostName ?: alias,
                    user = user,
                    port = port,
                    identityFile = identityFile,
                )
            )
            currentAlias = null
            hostName = null
            user = null
            port = null
            identityFile = null
        }

        configFile.readLines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            val parts = line.split(Regex("\\s+"), limit = 2)
            if (parts.size < 2) return@forEach

            val key = parts[0].lowercase()
            val value = parts[1]

            when (key) {
                "host" -> {
                    flushEntry()
                    currentAlias = value
                }
                "hostname" -> hostName = value
                "user" -> user = value
                "port" -> port = value.toIntOrNull()
                "identityfile" -> identityFile = value.replace("~", System.getProperty("user.home"))
            }
        }
        flushEntry()

        return entries
    }
}
