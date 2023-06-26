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
        implementation("org.apache.kafka:kafka-streams:3.5.0")
        implementation("org.slf4j:slf4j-api:2.0.7")
//        implementation("ch.qos.logback:logback-classic:1.4.8")
        implementation("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        implementation("com.google.code.gson:gson:2.10.1")
    }
}

