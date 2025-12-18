import com.gradleup.librarian.gradle.Librarian
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

fun Project.module() {
  Librarian.module(this)

  val publishing = extensions.getByName("publishing") as PublishingExtension

  publishing.repositories {
    it.maven {
      it.name = "PluginTest"
      it.url = uri(rootDir.resolve("build/m2"))
    }
  }

  extensions.findByType(KotlinJvmProjectExtension::class.java)?.apply {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
  }
}
