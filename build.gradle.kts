import com.gradleup.librarian.gradle.librarianRoot

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("build-logic:build-logic")
    }
}

librarianRoot()