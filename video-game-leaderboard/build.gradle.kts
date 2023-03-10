plugins {
    id("java")
    application
}

group = "com.icloud"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.icloud.App")
}

dependencies {
    implementation("io.javalin:javalin:5.3.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
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
            "stateDir" to "/tmp/kafka-streams1"
    )
}