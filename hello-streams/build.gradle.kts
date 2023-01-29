plugins {
    id("java")
}

group = "com.icloud"


task("runDSL", JavaExec::class) {
    mainClass.set("com.icloud.DslExample")
    classpath(sourceSets.main.get().runtimeClasspath)
}

task("runProcessorAPI", JavaExec::class) {
    mainClass.set("com.icloud.ProcessorApiExample")
    classpath(sourceSets.main.get().runtimeClasspath)
}