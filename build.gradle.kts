import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
}

group = "page.matthewt.diskspaceinvestigator"
val appVersion: String by project
version = appVersion

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Fluent UI for Compose
    implementation("com.konyaco:fluent:0.0.1-dev.8")
    implementation("com.konyaco:fluent-icons-extended:0.0.1-dev.8")

    // Kotlinx Serialization (CBOR for compact session files, JSON for GitHub API)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Apache MINA SSHD (SSH/SFTP client)
    implementation("org.apache.sshd:sshd-core:2.14.0")
    implementation("org.apache.sshd:sshd-sftp:2.14.0")
    implementation("org.apache.sshd:sshd-common:2.14.0")

    // Bouncy Castle for modern SSH key formats (Ed25519, etc.)
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4-desktop:1.7.3")

    // Embedded SSHD for SSH tests
    testImplementation("org.apache.sshd:sshd-core:2.14.0")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xmx2g")
}

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("page/matthewt/diskspaceinvestigator")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            package page.matthewt.diskspaceinvestigator

            object BuildConfig {
                const val VERSION = "$appVersion"
                const val GITHUB_REPO = "MatthewTPage/disk-space-investigator"
            }
            """.trimIndent()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildConfig.map { layout.buildDirectory.dir("generated/buildconfig") })
}

tasks.named("compileKotlin") {
    dependsOn(generateBuildConfig)
}

compose.desktop {
    application {
        mainClass = "page.matthewt.diskspaceinvestigator.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)

            packageName = "Disk Space Investigator"
            packageVersion = appVersion
            description = "Analyze and manage disk space usage"
            vendor = "Disk Space Investigator"

            windows {
                menuGroup = "Disk Space Investigator"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }

            macOS {
                bundleID = "page.matthewt.diskspaceinvestigator"
                iconFile.set(project.file("src/main/resources/icon.icns"))
                // macOS requires MAJOR > 0 for packaging, so offset by 1
                packageVersion = appVersion.split(".").let {
                    "${it[0].toInt() + 1}.${it.drop(1).joinToString(".")}"
                }
            }

            linux {
                packageName = "disk-space-investigator"
                iconFile.set(project.file("src/main/resources/icon.png"))
            }

            includeAllModules = true
        }

        buildTypes.release {
            proguard {
                isEnabled = false
            }
        }

        jvmArgs("-Xmx4g", "-Dskiko.renderApi=SOFTWARE")
    }
}
