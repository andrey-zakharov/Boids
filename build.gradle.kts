plugins {
    application
    kotlin("jvm") version Versions.kotlinVersion
    id("com.stehno.natives") version "0.3.1"
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
    maven("https://jitpack.io")
}

val ktxVersion = "1.10.0-b1"
val gdxVersion = "1.10.0"

dependencies {
    compileOnly("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")

    implementation(files("libs/ktcl-0.3.1.jar"))

    implementation(DepsJvm.lwjgl())
    implementation(DepsJvm.lwjgl("glfw"))
    implementation(DepsJvm.lwjgl("opencl"))
    implementation(DepsJvm.lwjgl("opengl"))
    implementation(DepsJvm.lwjgl("stb"))

    runtimeOnly(DepsJvm.lwjglNatives())
    runtimeOnly(DepsJvm.lwjglNatives("stb"))
    runtimeOnly(DepsJvm.lwjglNatives("glfw"))
    runtimeOnly(DepsJvm.lwjglNatives("opengl"))


    implementation("io.github.libktx", "ktx-app", ktxVersion)
    implementation("io.github.libktx", "ktx-async", ktxVersion)

    implementation("com.badlogicgames.gdx", "gdx", gdxVersion)
    implementation("com.badlogicgames.gdx", "gdx-backend-lwjgl3", gdxVersion)
    api("com.badlogicgames.gdx","gdx-platform", gdxVersion, classifier = "natives-desktop")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    //testImplementation(project(path=""))
    testImplementation("io.github.libktx", "ktx-app", ktxVersion)
    testImplementation("com.badlogicgames.gdx", "gdx", gdxVersion)
    testImplementation("com.badlogicgames.gdx", "gdx-backend-headless", gdxVersion)
//    testImplementation("com.badlogicgames.gdx", "gdx-backend-lwjgl3", gdxVersion)
    testImplementation("com.badlogicgames.gdx","gdx-platform", gdxVersion, classifier = "natives-desktop")

    testRuntimeOnly(DepsJvm.lwjglNatives())
    testRuntimeOnly(DepsJvm.lwjglNatives("stb"))
    testRuntimeOnly(DepsJvm.lwjglNatives("glfw"))
    testRuntimeOnly(DepsJvm.lwjglNatives("opengl"))
    testRuntimeOnly(DepsJvm.lwjglNatives("openal"))
    //testImplementation("com.badlogicgames.gdx", "gdx-box2d", gdxVersion)
    //testImplementation("com.badlogicgames.gdx","gdx-box2d-platform", gdxVersion, classifier = "natives-desktop")
}

application {
    // Define the main class for the application.
    mainClassName = "me.zakharov.GameKt"
}
/*
jar {
    manifest {
        attributes 'Main-Class': 'Game'
    }
}*/

tasks.withType<Test> {
    this.testLogging {
        this.showStandardStreams = true
    }
}