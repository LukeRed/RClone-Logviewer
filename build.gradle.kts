import org.gradle.jvm.tasks.Jar

plugins {
    java
    kotlin("jvm") version "1.3.41"
}
group = "eu.redasurc"
version = "1.0-SNAPSHOT"

dependencies {
    compile(kotlin("stdlib"))
}

repositories {
    jcenter()
}

tasks {
    register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(mapOf("Main-Class" to "LogViewerKt"))
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}