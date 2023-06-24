plugins {
    java
}

subprojects {
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
        implementation("org.apache.kafka:kafka-streams:3.3.2")
    }
}

