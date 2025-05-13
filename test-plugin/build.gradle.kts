buildscript {
    repositories {
        mavenCentral()
        google()
        maven("https://storage.googleapis.com/gradleup/m2")
    }
    dependencies {
        classpath("gratatouille-build-logic:gratatouille-build-logic")
        classpath(libs.ksp.gradle.plugin)
        classpath("com.gradleup.gratatouille:gratatouille-gradle-plugin")
    }
}
