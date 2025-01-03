import com.gradleup.librarian.gradle.librarianModule
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension

fun Project.module() {
  librarianModule()

  val publishing = extensions.getByName("publishing") as PublishingExtension

  publishing.repositories {
    it.maven {
      it.name = "PluginTest"
      it.url = uri(rootDir.resolve("build/m2"))
    }
  }
}