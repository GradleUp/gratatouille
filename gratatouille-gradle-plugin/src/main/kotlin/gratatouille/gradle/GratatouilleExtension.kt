package gratatouille.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy

abstract class GratatouilleExtension(private val project: Project) {
  fun plugin(id: String, action: Action<PluginSpec>) {
    val spec = PluginSpec(id, project)
    action.execute(spec)
  }
}

