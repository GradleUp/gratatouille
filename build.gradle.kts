import com.gradleup.librarian.gradle.Librarian

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("build-logic:build-logic")
    }
}

Librarian.root(project)

tasks.register("publishAllPublicationsToPluginTestRepository") {
    subprojects {
        dependsOn("$path:publishAllPublicationsToPluginTestRepository")
    }
}

