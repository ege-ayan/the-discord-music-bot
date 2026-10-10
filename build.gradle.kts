import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest

plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
}

group = "io.github.egeayan"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    implementation(libs.jda) {
        exclude(module = "opus-java")
    }
    implementation(libs.jdave.api)
    runtimeOnly(libs.bundles.jdave.natives)

    implementation(libs.lavaplayer)
    implementation(libs.youtube.source)

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

abstract class DownloadYtDlp : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val assets: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val target = outputDirectory.get().dir("yt-dlp").asFile
        target.deleteRecursively()
        target.mkdirs()
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        fun fetch(name: String): ByteArray {
            val uri = URI.create("https://github.com/yt-dlp/yt-dlp/releases/download/${version.get()}/$name")
            val response = client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofByteArray())
            check(response.statusCode() == 200) { "Downloading $uri failed with HTTP ${response.statusCode()}" }
            return response.body()
        }
        val checksums = fetch("SHA2-256SUMS").decodeToString().lines()
            .map { it.trim().split(Regex("\\s+"), limit = 2) }
            .filter { it.size == 2 }
            .associate { (hash, file) -> file.removePrefix("*") to hash.lowercase() }
        for (asset in assets.get()) {
            val bytes = fetch(asset)
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            check(actual == checksums[asset]) { "Checksum mismatch for yt-dlp asset $asset" }
            target.resolve(asset).writeBytes(bytes)
        }
        target.resolve("VERSION").writeText(version.get())
    }
}

val downloadYtDlp = tasks.register<DownloadYtDlp>("downloadYtDlp") {
    version = "2026.08.19"
    assets = listOf("yt-dlp_macos", "yt-dlp_linux", "yt-dlp_linux_aarch64", "yt-dlp.exe")
    outputDirectory = layout.buildDirectory.dir("generated/yt-dlp")
}

sourceSets.main {
    resources.srcDir(downloadYtDlp)
}

application {
    mainClass = "io.github.egeayan.musicbot.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}", "-Xshare:off")
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.shadowJar {
    archiveClassifier = "all"
    filesMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
    manifest {
        attributes("Enable-Native-Access" to "ALL-UNNAMED")
    }
}

spotless {
    java {
        palantirJavaFormat("2.102.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
