java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
repositories {
    maven(url = "https://packages.confluent.io/maven/")
}

dependencies {
    implementation("io.confluent.ksql:ksqldb-udf:7.4.0")
}