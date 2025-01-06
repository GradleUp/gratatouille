package gratatouille.gradle

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal

internal fun MavenPublication.trySetAlias() {
  try {
    this as MavenPublicationInternal
    this.isAlias = true
  } catch (_: Exception) {

  }
}