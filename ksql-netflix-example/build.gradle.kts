plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "com.icloud"

repositories {
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

dependencies {
    implementation("io.confluent.ksql:ksqldb-udf:7.3.2")
}
