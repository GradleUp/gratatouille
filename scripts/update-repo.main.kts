#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
//@file:Repository("file://~/.m2/repository")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT-0cbca80f60a15a0f851a9cd468bfc352db316dd4")

import com.gradleup.librarian.cli.updateRepo

updateRepo(args) {
  file("README.md") {
    replacePluginVersion("com.gradleup.gratatouille")
    replacePluginVersion("com.gradleup.gratatouille.tasks")
    replacePluginVersion("com.gradleup.gratatouille")
  }
}
