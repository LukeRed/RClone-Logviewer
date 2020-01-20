import org.gradle.jvm.tasks.Jar
plugins {
    java
    kotlin("jvm") version "1.3.41"
    id("org.openjfx.javafxplugin") version "0.0.8"
}
group = "eu.redasurc"
version = "1.1-SNAPSHOT"

buildscript {
    repositories {
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.openjfx:javafx-plugin:0.0.8")
    }
}
apply(plugin = "org.openjfx.javafxplugin")

javafx {
    modules("javafx.controls", "javafx.fxml")
}

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