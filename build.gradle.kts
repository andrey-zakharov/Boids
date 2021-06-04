/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

plugins {
    // Apply the application plugin to add support for building a CLI application.
    application
    kotlin("jvm") version "1.5.10"
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
val lwjglNatives = "natives-windows"
val lwjglVersion = "3.2.3"
val ktxVersion = "1.10.0-b1"
val gdxVersion = "1.10.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(files("../ktcl/build/libs/ktcl-0.3.1.jar"))

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opencl")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("io.github.libktx", "ktx-app", ktxVersion)
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
    testImplementation("com.badlogicgames.gdx", "gdx", gdxVersion)
    testImplementation("com.badlogicgames.gdx", "gdx-backend-headless", gdxVersion)
    testImplementation("com.badlogicgames.gdx","gdx-platform", gdxVersion, classifier = "natives-desktop")

    //testImplementation("com.badlogicgames.gdx", "gdx-box2d", gdxVersion)
    //testImplementation("com.badlogicgames.gdx","gdx-box2d-platform", gdxVersion, classifier = "natives-desktop")
}

application {
    // Define the main class for the application.
    mainClassName = "Ants.AppKt"
}
