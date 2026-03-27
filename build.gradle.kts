import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
}

group = "page.matthewt.diskspaceinvestigator"
version = "1.0.0"

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

    // Kotlinx Serialization (CBOR for compact session files)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.7.3")

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

compose.desktop {
    application {
        mainClass = "page.matthewt.diskspaceinvestigator.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "Disk Space Investigator"
            packageVersion = "1.0.0"
            description = "Analyze and manage disk space usage"
            vendor = "Disk Space Investigator"

            windows {
                menuGroup = "Disk Space Investigator"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            }

            macOS {
                bundleID = "page.matthewt.diskspaceinvestigator"
            }

            linux {
                packageName = "disk-space-investigator"
            }
        }

        buildTypes.release {
            proguard {
                isEnabled = false
            }
        }

        jvmArgs("-Xmx4g")
    }
}
