plugins {
    java
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
        implementation("org.apache.kafka:kafka-streams:3.4.0")
        if (project.name != "json-serde" && project.name != "ksql-netflix-example") {
            implementation("org.slf4j:slf4j-api:1.7.3")
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("org.projectlombok:lombok:$lombokVersion")
            annotationProcessor("org.projectlombok:lombok:$lombokVersion")
            implementation(project(":json-serde"))
        }
    }
}

