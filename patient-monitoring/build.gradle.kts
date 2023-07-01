import java.util.concurrent.Executors

plugins {
    application
}

dependencies {
    implementation(project(":common"))
    implementation("io.javalin:javalin:5.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

application {
    mainClass.set("com.icloud.App")
}

task("runFirst", JavaExec::class) {
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    systemProperties = mapOf(
            "host" to "localhost",
            "port" to "8080",
            "stateDir" to "/tmp/kafka-streams"
    )
}

task("runSecond", JavaExec::class) {
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    systemProperties = mapOf(
            "host" to "localhost",
            "port" to "8090",
            "stateDir" to "/tmp/kafka-streams2"
    )
}