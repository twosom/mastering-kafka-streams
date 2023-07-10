rootProject.name = "mastering-kafka"
include("hello-streams")
include("cripto-sentiment")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
include("common")
include("video-game-leaderboard")
include("patient-monitoring")
include("digital-twin")
include("ksqldb-server")
