plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "the-discord-music-bot"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven("https://maven.lavalink.dev/releases") {
            content {
                includeGroupAndSubgroups("dev.lavalink")
                includeGroupAndSubgroups("dev.arbjerg")
            }
        }
    }
}
