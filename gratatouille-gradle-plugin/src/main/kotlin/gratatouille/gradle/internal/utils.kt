package gratatouille.gradle.internal

import com.google.devtools.ksp.gradle.KspExtension
import com.gradleup.gratatouille.gradle.BuildConfig
import gratatouille.gradle.VERSION
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension


internal fun String.displayName() =
    this.split(".").joinToString(separator = "") { it.replaceFirstChar { it.uppercase() } }

internal val Project.kotlinExtension: KotlinJvmProjectExtension
    get() {
        val kotlin = project.extensions.findByType(KotlinJvmProjectExtension::class.java)
        check(kotlin != null) {
            "Gratatouille requires the 'org.jetbrains.kotlin.jvm' plugin to be applied"
        }
        return kotlin
    }


internal val Project.kspExtension: KspExtension
    get() {
        val kotlin = project.extensions.findByType(KspExtension::class.java)
        check(kotlin != null) {
            "Gratatouille require the 'com.google.devtools.ksp' plugin to be applied"
        }
        return kotlin
    }

internal val USAGE_GRATATOUILLE = "gratatouille"
