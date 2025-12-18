#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.1.1-SNAPSHOT-44a36034d83f8ba9c9177653f29df3dea933fc3f")

import com.gradleup.librarian.cli.updateRepo

updateRepo(args) {
  file("README.md") {
    replacePluginVersion("com.gradleup.gratatouille")
    replacePluginVersion("com.gradleup.gratatouille.tasks")
    replacePluginVersion("com.gradleup.gratatouille")
  }
}
