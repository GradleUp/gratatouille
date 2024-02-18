buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("build-logic:build-logic")
    }
}

group = "sample-plugin"