buildscript {
    repositories {
        mavenCentral()

        maven("https://storage.googleapis.com/gradleup/m2")
    }
    dependencies {
        classpath("build-logic:build-logic")
        classpath(libs.ksp.gradle.plugin)
        classpath("com.gradleup.gratatouille:gratatouille-gradle-plugin")
    }
}

