plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.gradleup.gratatouille")
    id("maven-publish")
}

version = "0.0.0"

publishing {
    publications {
        create("default", MavenPublication::class.java) {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "pluginTest"
            url = uri(rootDir.resolve("../build/m2"))
        }
    }
}

dependencies {
    gratatouille(project(":implementation"))
}

dependencies {
    compileOnly(libs.gradle.api)
    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test)
}


gratatouille {
    pluginMarker("testplugin.isolated", "default")
    codeGeneration {

    }
}

tasks.withType(Test::class.java) {
    dependsOn("publishAllPublicationsToPluginTestRepository")
    dependsOn(":implementation:publishAllPublicationsToPluginTestRepository")
    dependsOn(gradle.includedBuild("gratatouille").task(":publishAllPublicationsToPluginTestRepository"))
}