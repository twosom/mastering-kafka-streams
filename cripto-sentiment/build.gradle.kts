import java.net.URI

plugins {
    id("com.github.davidmc24.gradle.plugin.avro") version "1.7.1"
}

// TODO 추후 해당 의존성은 common 모듈로 옮길 수도 있음.
repositories {
    maven {
        url = URI("https://packages.confluent.io/maven")
    }
}
dependencies {
    implementation("io.confluent:kafka-streams-avro-serde:7.4.0")
    implementation("com.mitchseymour:kafka-registryless-avro-serdes:0.1.0")
    implementation("org.apache.avro:avro:1.11.1")
}

