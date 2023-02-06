plugins {
    id("java")
    application
}

group = "com.icloud"

application {
    mainClass.set("com.icloud.App")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:5.3.2")
}

task("runFirst", JavaExec::class) {
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    systemProperties = mapOf(
            "host" to "localhost",
            "port" to "8000",
            "stateDir" to "/tmp/kafka-streams"
    )
}

task("runSecond", JavaExec::class) {
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    systemProperties = mapOf(
            "host" to "localhost",
            "port" to "8100",
            "stateDir" to "/tmp/kafka-streams2"
    )
}
