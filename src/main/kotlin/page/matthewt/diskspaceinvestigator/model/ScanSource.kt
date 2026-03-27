package page.matthewt.diskspaceinvestigator.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ScanSource {
    abstract val path: String
    abstract val displayName: String

    @Serializable
    @SerialName("local")
    data class Local(override val path: String) : ScanSource() {
        override val displayName: String get() = path
    }

    @Serializable
    @SerialName("ssh")
    data class Ssh(
        val host: String,
        val user: String,
        override val path: String,
        val port: Int = 22,
    ) : ScanSource() {
        override val displayName: String get() = "$user@$host:$path"
    }
}
