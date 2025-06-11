package gratatouille.wiring

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.lang.AutoCloseable
import java.net.URI
import java.net.URLClassLoader

abstract class GratatouilleBuildService: BuildService<BuildServiceParameters.None>, AutoCloseable {
  private val classloaders = mutableMapOf<List<URI>, ClassLoader>()

  @Synchronized
  fun classloader(classpath: Set<File>): ClassLoader {
    val urls = classpath.map { it.toURI() }
    return classloaders.getOrPut(urls) {
      URLClassLoader(
          urls.map { it.toURL() }.toTypedArray(),
          ClassLoader.getPlatformClassLoader()
      )
    }
  }

  @Synchronized
  override fun close() {
    classloaders.clear()
  }
}