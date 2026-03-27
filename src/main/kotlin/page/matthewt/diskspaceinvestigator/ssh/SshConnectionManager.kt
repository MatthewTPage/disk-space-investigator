package page.matthewt.diskspaceinvestigator.ssh

import page.matthewt.diskspaceinvestigator.model.ScanSource
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.keyprovider.FileKeyPairProvider
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import java.io.File
import java.nio.file.Path
import java.security.KeyPair

class SshConnectionManager {

    data class AuthCallbacks(
        val onPassphraseNeeded: suspend (keyFile: String) -> String?,
        val onPasswordNeeded: suspend (host: String) -> String?,
    )

    private var client: SshClient? = null
    private var session: ClientSession? = null

    fun discoverKeys(): List<Path> {
        val sshDir = File(System.getProperty("user.home"), ".ssh")
        if (!sshDir.isDirectory) return emptyList()

        val keyNames = listOf("id_ed25519", "id_rsa", "id_ecdsa", "id_dsa")
        return keyNames
            .map { sshDir.resolve(it) }
            .filter { it.exists() && it.isFile }
            .map { it.toPath() }
    }

    suspend fun connect(
        source: ScanSource.Ssh,
        callbacks: AuthCallbacks,
    ): SftpClient {
        val sshClient = SshClient.setUpDefaultClient()
        sshClient.start()
        this.client = sshClient

        val hostEntry = SshConfig.parseConfig().find {
            it.alias == source.host || it.hostName == source.host
        }

        val actualHost = hostEntry?.hostName ?: source.host
        val actualUser = source.user.ifBlank { hostEntry?.user ?: System.getProperty("user.name") }
        val actualPort = if (source.port != 22) source.port else (hostEntry?.port ?: 22)

        val clientSession = sshClient.connect(actualUser, actualHost, actualPort)
            .verify(30_000)
            .session

        this.session = clientSession

        // Try key-based auth
        var authenticated = false
        val keys = discoverKeys()

        for (keyPath in keys) {
            try {
                val keyPairs = loadKeyPair(keyPath, callbacks)
                if (keyPairs != null) {
                    clientSession.addPublicKeyIdentity(keyPairs)
                    clientSession.auth().verify(30_000)
                    authenticated = true
                    break
                }
            } catch (e: Exception) {
                // Key failed, try next
                continue
            }
        }

        // Fallback to password auth
        if (!authenticated) {
            val password = callbacks.onPasswordNeeded(source.host)
                ?: throw IllegalStateException("Authentication cancelled")
            clientSession.addPasswordIdentity(password)
            clientSession.auth().verify(30_000)
        }

        return SftpClientFactory.instance().createSftpClient(clientSession)
    }

    private suspend fun loadKeyPair(keyPath: Path, callbacks: AuthCallbacks): KeyPair? {
        return try {
            val provider = FileKeyPairProvider(keyPath)
            val pairs = provider.loadKeys(null).toList()
            pairs.firstOrNull()
        } catch (e: Exception) {
            // Key might be passphrase-protected
            val passphrase = callbacks.onPassphraseNeeded(keyPath.fileName.toString()) ?: return null
            try {
                val loader = org.apache.sshd.common.config.keys.loader.openssh.OpenSSHKeyPairResourceParser.INSTANCE
                val passwordProvider = org.apache.sshd.common.config.keys.FilePasswordProvider { _, _, _ -> passphrase }
                val pairs = loader.loadKeyPairs(null, keyPath, passwordProvider)
                pairs.firstOrNull()
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun disconnect() {
        try {
            session?.close()
            client?.stop()
        } catch (_: Exception) {
        } finally {
            session = null
            client = null
        }
    }
}
