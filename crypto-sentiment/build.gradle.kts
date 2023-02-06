import net.ltgt.gradle.errorprone.errorprone
import java.nio.charset.StandardCharsets

plugins {
    id("java")
    id("application")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
    id("net.ltgt.errorprone") version "3.0.1"
}

group = "com.icloud"

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
}

avro {
    fieldVisibility.set("PRIVATE")
}


dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.18.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.confluent:kafka-streams-avro-serde:7.3.1") {
        exclude("org.apache.kafka:kafka-clients")
    }
    implementation("org.apache.avro:avro:1.11.1")
    implementation("com.mitchseymour:kafka-registryless-avro-serdes:1.0.0")
}
tasks {
    compileJava {
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = StandardCharsets.UTF_8.name()
        options.errorprone {
            disableWarningsInGeneratedCode.set(true)
            excludedPaths.set(".*/build/generated/.*")
        }
    }
}