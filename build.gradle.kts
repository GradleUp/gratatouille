buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("build-logic:build-logic")
    }
}

group = "com.gradleup.gratatouille"
version = "0.1"

configureRoot()