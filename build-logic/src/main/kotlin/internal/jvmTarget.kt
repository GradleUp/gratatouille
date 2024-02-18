package internal
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.targetJdk(jdk: Int) {
    tasks.withType(JavaCompile::class.java) {
        it.options.release.set(jdk)
    }

    tasks.withType(KotlinCompile::class.java) {
        it.kotlinOptions.jvmTarget = jdk.toString()
    }
}