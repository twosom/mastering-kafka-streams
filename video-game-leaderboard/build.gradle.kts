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
    implementation("org.slf4j:slf4j-api:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.google.code.gson:gson:2.10.1")
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