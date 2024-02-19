package gratatouille.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import javax.inject.Inject

class GratatouilleGradlePluginPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configurations.create("gratatouille") {
            it.isTransitive = false
        }
        target.withPlugins("org.jetbrains.kotlin.jvm") {
            target.configure()
        }
    }

    private fun Project.configure() {
        val extractSources = tasks.register("extractGratatouilleSources", ExtractGratatouilleSources::class.java) { task ->
            task.from(configurations.getByName("gratatouille").elements.map { it.map { task.getArchiveOperations().zipTree(it) } })

            task.include("META-INF/gratatouille/**")
            task.eachFile {
                it.path = it.sourcePath.removePrefix("META-INF/gratatouille")
            }
            task.includeEmptyDirs = false
            task.into(layout.buildDirectory.dir("extractGratatouilleSources/sources"))
            task.doFirst {
                task.destinationDir.apply {
                    deleteRecursively()
                    mkdirs()
                }
            }
        }

        extensions.getByName("kotlin").apply {
            this as KotlinJvmProjectExtension
            sourceSets.getByName("main").kotlin.srcDir(extractSources)
        }
    }
}

abstract class ExtractGratatouilleSources: Copy() {
    @Inject
    abstract fun getArchiveOperations(): ArchiveOperations
}