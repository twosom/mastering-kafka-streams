plugins {
    id("java")
}
val lombokVersion: String = "1.18.22"
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
        implementation("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    }
}

