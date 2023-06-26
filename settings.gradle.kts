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
