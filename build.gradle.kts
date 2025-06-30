import com.gradleup.librarian.gradle.Librarian

plugins {
    id("base")
}
buildscript {

    dependencies {
        classpath("gratatouille-build-logic:gratatouille-build-logic:unused")
    }
}

Librarian.root(project)

tasks.register("publishAllPublicationsToPluginTestRepository") {
    subprojects {
        dependsOn("$path:publishAllPublicationsToPluginTestRepository")
    }
}

