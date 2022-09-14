import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
}

group = "com.github.devlaq"
version = "1.0-SNAPSHOT"

val mindustryVersion: String by project

repositories {
    mavenCentral()
    maven("https://www.jitpack.io")
}

dependencies {
    testCompileOnly(kotlin("test"))

    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.Anuken.Arc:flabel:$mindustryVersion")
    compileOnly("com.github.Anuken.mindustryjitpack:core:$mindustryVersion")
    compileOnly("com.github.Anuken.mindustryjitpack:server:$mindustryVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-FAT")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}