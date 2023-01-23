import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    `maven-publish`
}

group = "com.github.devlaq"
version = "1.0-SNAPSHOT"

val mindustryVersion: String by project
val testServerDirectory: String? by project

val pluginVersion: String? by project

/**
 * Generate plugin.json
 */
fun generatePluginMeta() {
    val template = File("./plugin.template.json")
    val dest = File("./src/main/resources/plugin.json")

    val placeholders = mapOf<String, Any?>(
        "version" to pluginVersion
    )

    var replaced = template.readText()
    placeholders.forEach { (t, u) ->
        replaced = replaced.replace("\${${t}}", u.toString())
    }

    if(!dest.exists()) dest.createNewFile()
    dest.writeText(replaced)
}
generatePluginMeta()

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.devlaq.arkitect"
            artifactId = "Arkitect"
            version = pluginVersion

            from(components["java"])
        }
    }
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

dependencies {
    testCompileOnly(kotlin("test"))

    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.Anuken.Arc:flabel:$mindustryVersion")
    compileOnly("com.github.Anuken.Mindustry:core:$mindustryVersion")
    compileOnly("com.github.Anuken.Mindustry:server:$mindustryVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-FAT")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

task("generateSources") {
    generatePluginMeta()
}

if(System.getenv("JITPACK") != "true") {
    task("testFatJar") {
        dependsOn(fatJar)
        if(!fatJar.archiveFile.get().asFile.exists()) return@task
        val buildDir = layout.buildDirectory
        val builtFatJar = buildDir.dir("libs").get().file(fatJar.archiveFileName.get()).asFile
        val destFile = File("${testServerDirectory}/config/mods/Arkitect-TEST.jar")
        if(testServerDirectory != null) {
            doLast {
                builtFatJar.copyTo(destFile, overwrite = true)

                fun getJvmProcesses(): MutableMap<Int, String> {
                    val jpsProcess = ProcessBuilder()
                        .command("/usr/lib/jvm/jdk-17/bin/jps", "-l")
                        .start()
                    jpsProcess.waitFor()
                    val output = jpsProcess.inputReader().readText()
                    val map = mutableMapOf<Int, String>()
                    output.lines().forEach {
                        val split = it.split(" ", limit = 2)
                        if(split.size > 1) map[split[0].toInt()] = split[1]
                    }
                    return map
                }
                fun killAliveTestServers() {
                    val jvmProcesses = getJvmProcesses().filterValues { it.startsWith(testServerDirectory!!) }
                    val processesToKill = jvmProcesses.keys.joinToString(" ") { it.toString() }
                    println("Killed alive test servers [${jvmProcesses.keys.joinToString(",")}]")
                    val process = ProcessBuilder()
                        .command("kill", "-9", processesToKill)
                        .start()
                    process.waitFor()
                }
                killAliveTestServers()
                val process = ProcessBuilder()
                    .command("gnome-terminal", "--title", "Mindustry v$mindustryVersion Headless [Arkitect Testing Server]", "--wait", "--", "sh", "${testServerDirectory}/start.sh")
                    .directory(File(testServerDirectory!!))
                    .start()
                process.waitFor()
            }
        }
    }
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
