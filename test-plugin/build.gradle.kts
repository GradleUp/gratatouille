buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("build-logic:build-logic")
        classpath(libs.ksp.gradle.plugin)
        classpath("com.gradleup.gratatouille:gratatouille-gradle-plugin")
    }
}
