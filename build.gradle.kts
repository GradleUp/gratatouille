import com.gradleup.librarian.gradle.Librarian

plugins {
    id("base")
}
buildscript {
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

