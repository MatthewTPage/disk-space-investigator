package page.matthewt.diskspaceinvestigator.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import page.matthewt.diskspaceinvestigator.BuildConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val fileName: String,
)

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<GitHubAsset>,
)

@Serializable
private data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
)

object UpdateChecker {

    private val json = Json { ignoreUnknownKeys = true }

    private val assetSuffix: String? = when {
        System.getProperty("os.name").lowercase().contains("win") -> ".msi"
        System.getProperty("os.name").lowercase().contains("mac") -> ".dmg"
        System.getProperty("os.name").lowercase().contains("linux") -> {
            // Prefer .deb, fall back to .rpm
            ".deb"
        }
        else -> null
    }

    fun check(): UpdateInfo? {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"))
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null

        val release = json.decodeFromString<GitHubRelease>(response.body())
        val latestVersion = release.tag_name.removePrefix("v")

        if (!isNewer(latestVersion, BuildConfig.VERSION)) return null

        val asset = release.assets.firstOrNull { it.name.endsWith(assetSuffix ?: return null) }
            ?: return null

        return UpdateInfo(
            version = latestVersion,
            downloadUrl = asset.browser_download_url,
            releaseUrl = release.html_url,
            fileName = asset.name,
        )
    }

    fun downloadAndInstall(update: UpdateInfo, onProgress: (String) -> Unit) {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        onProgress("Downloading ${update.fileName}...")

        val request = HttpRequest.newBuilder()
            .uri(URI.create(update.downloadUrl))
            .GET()
            .build()

        val tempDir = Files.createTempDirectory("dsi-update")
        val installerPath = tempDir.resolve(update.fileName)

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { input ->
            Files.copy(input, installerPath, StandardCopyOption.REPLACE_EXISTING)
        }

        onProgress("Launching installer...")
        launchInstaller(installerPath)
    }

    private fun launchInstaller(path: Path) {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("win") -> listOf("msiexec", "/i", path.toString(), "/qn")
            os.contains("mac") -> listOf("open", path.toString())
            else -> if (path.toString().endsWith(".deb")) {
                listOf("pkexec", "dpkg", "-i", path.toString())
            } else {
                listOf("pkexec", "rpm", "-U", path.toString())
            }
        }
        ProcessBuilder(command).start()
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
