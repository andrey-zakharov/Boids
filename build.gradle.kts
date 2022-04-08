plugins {
    // Apply the application plugin to add support for building a CLI application.
    application
    kotlin("jvm") version "1.6.10"
    id("com.stehno.natives") version "0.3.1"
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
    maven("https://jitpack.io")
}
val lwjglNatives = "natives-linux"
val lwjglVersion = "3.2.3"
val ktxVersion = "1.10.0-b1"
val gdxVersion = "1.10.0"

dependencies {
    compileOnly("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")

    implementation(files("libs/ktcl-0.3.1.jar"))

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opencl")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")

    implementation("io.github.libktx", "ktx-app", ktxVersion)
    implementation("io.github.libktx", "ktx-async", ktxVersion)

    implementation("com.badlogicgames.gdx", "gdx", gdxVersion)
    implementation("com.badlogicgames.gdx", "gdx-backend-lwjgl3", gdxVersion)
    api("com.badlogicgames.gdx","gdx-platform", gdxVersion, classifier = "natives-desktop")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    //testImplementation(project(path=""))
    testImplementation("io.github.libktx", "ktx-app", ktxVersion)
    testImplementation("com.badlogicgames.gdx", "gdx", gdxVersion)
    testImplementation("com.badlogicgames.gdx", "gdx-backend-headless", gdxVersion)
//    testImplementation("com.badlogicgames.gdx", "gdx-backend-lwjgl3", gdxVersion)
    testImplementation("com.badlogicgames.gdx","gdx-platform", gdxVersion, classifier = "natives-desktop")
    testRuntimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    testRuntimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    testRuntimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    testRuntimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    testRuntimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
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