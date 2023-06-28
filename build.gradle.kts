import java.net.URI

plugins {
    java
}
val lombokVersion: String = "1.18.22"


subprojects {
    repositories {
        maven {
            url = URI("https://packages.confluent.io/maven")
        }
    }
    apply {
        plugin("java")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation("com.mitchseymour:kafka-registryless-avro-serdes:0.1.0")
        implementation("io.confluent:kafka-streams-avro-serde:7.4.0") {
            exclude(group = "org.apache.kafka", module = "kafka-client")
        }
        implementation("org.apache.kafka:kafka-streams:3.5.0")
        implementation("org.slf4j:slf4j-api:2.0.7")
        implementation("org.slf4j:slf4j-simple:2.0.7")
        implementation("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("com.google.code.gson:gson:2.10.1")
    }
}

