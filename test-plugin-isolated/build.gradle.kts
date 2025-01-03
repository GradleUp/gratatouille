buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("build-logic:build-logic")
        classpath("com.gradleup.gratatouille:gratatouille-gradle-plugin")
    }
}

