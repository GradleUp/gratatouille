package internal

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import kotlin.time.Duration.Companion.minutes


internal enum class SonatypeHost {
    Ossrh,
    OssrhS01,
}


internal class SonatypeOptions(
    val username: String,
    val password: String,
    val host: SonatypeHost,
    val stagingProfile: String
)

internal class ProjectOptions(
    val groupId: String,
    val version: String,
    val descriptions: String,
    val vcsUrl: String,
    val developers: String,
    val license: String,
    val licenseUrl: String,
)

internal class SigningOptions(
    /**
     * GPG_PRIVATE_KEY must contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
     * It can be obtained with gpg --armour --export-secret-keys KEY_ID
     */
    val privateKey: String,
    val privateKeyPassword: String
)

internal class GithubOptions(
    val mainBranch: String,
    val autoRelease: Boolean
)

/**
 * Configures publishing
 *
 * @param projectOptions options for coordinates and POM
 * @param sonatypeOptions sonatype options
 * @param signingOptions signing options. May be null to skip signing
 */
internal fun Project.configurePublishing(
    projectOptions: ProjectOptions,
    sonatypeOptions: SonatypeOptions?,
    signingOptions: SigningOptions?,
    configurePublications: Action<PublicationContainer> = Action {  }
) {
    group = projectOptions.groupId
    version = projectOptions.version

    plugins.apply("maven-publish")

    val publishing = extensions.getByType(PublishingExtension::class.java)
    configurePublications.execute(publishing.publications)

    if (sonatypeOptions != null) {
        publishing.repositories {
            it.mavenSonatypeSnapshot(sonatypeOptions)
            it.mavenSonatypeStaging(sonatypeOptions = sonatypeOptions, project = project)
        }

        rootProject.publishSnapshotsTaskProvider().configure {
            it.dependsOn(tasks.named("publishAllPublicationsToOssSnapshotsRepository"))
        }
        rootProject.publishStagingTaskProvider().configure {
            it.dependsOn(tasks.named("publishAllPublicationsToOssStagingRepository"))
        }
    }
    val emptyJavadocJarTaskProvider = tasks.register("libEmptyJavadocJar", Jar::class.java) {
        it.archiveClassifier.set("javadoc")
    }

    val emptySourcesJarTaskProvider = tasks.register("libEmptySourcesJar", Jar::class.java) {
        it.archiveClassifier.set("sources")
    }
    afterEvaluate {
        publishing.publications.configureEach {
            (it as MavenPublication)
            if (it.pom.packaging != "pom") {
                if (it.artifacts.none { it.classifier == "javadoc" }) {
                    it.artifact(emptyJavadocJarTaskProvider)
                }
                if (it.artifacts.none { it.classifier == "sources" }) {
                    it.artifact(emptySourcesJarTaskProvider)
                }
            }
            it.pom {
                it.name.set(name)
                it.description.set(projectOptions.descriptions)
                it.url.set(projectOptions.vcsUrl)
                it.scm {
                    it.url.set(projectOptions.vcsUrl)
                    it.connection.set(projectOptions.vcsUrl)
                    it.developerConnection.set(projectOptions.vcsUrl)
                }
                it.licenses {
                    it.license {
                        it.name.set(projectOptions.license)
                        it.url.set(projectOptions.licenseUrl)
                    }
                }
                it.developers {
                    it.developer {
                        it.id.set(projectOptions.developers)
                        it.name.set(projectOptions.developers)
                    }
                }
            }
        }
    }

    plugins.apply("signing")
    extensions.getByType(SigningExtension::class.java).apply {
        // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
        // It can be obtained with gpg --armour --export-secret-keys KEY_ID
        useInMemoryPgpKeys(System.getenv("GPG_KEY"), System.getenv("GPG_KEY_PASSWORD"))
        sign(publishing.publications)
    }

//    if (signingOptions != null) {
//        val signing = extensions.getByType(SigningExtension::class.java)
//
//        signing.useInMemoryPgpKeys(System.getenv("GPG_KEY"), System.getenv("GPG_KEY_PASSWORD"))
//        signing.sign(publishing.publications)
//
//        // See https://github.com/gradle/gradle/issues/26091
//        tasks.withType(AbstractPublishToMaven::class.java).configureEach {
//            val signingTasks = tasks.withType(Sign::class.java)
//            it.mustRunAfter(signingTasks)
//        }
//    }
}

/**
 * @param githubOptions CI options. May be null to skip publishing on CI. If non-null, creates `publishIfNeeded` task
 * that publishes to SNAPSHOTS if the version ends with `-SNAPSHOT` or Maven Central else
 */
internal fun Project.configureGitHub(projectOptions: ProjectOptions, sonatypeOptions: SonatypeOptions?, githubOptions: GithubOptions?) {
    if (githubOptions != null  && sonatypeOptions != null) {
        val publishIfNeeded = publishIfNeededTaskProvider()
        val publishStaging = publishStagingTaskProvider()
        val publishSnapshots = publishSnapshotsTaskProvider()
        val ossStagingReleaseTask = registerReleaseTask(sonatypeOptions, githubOptions.autoRelease, "ossStagingRelease")
        ossStagingReleaseTask.dependsOn(publishStaging)

        val eventName = System.getenv("GITHUB_EVENT_NAME")
        val ref = System.getenv("GITHUB_REF")

        if (eventName == "push" && ref == "refs/heads/${githubOptions.mainBranch}" && projectOptions.version.endsWith("-SNAPSHOT")
        ) {
            publishIfNeeded.dependsOn(publishSnapshots)
        }

        if (ref?.startsWith("refs/tags/") == true) {
            publishIfNeeded.dependsOn(ossStagingReleaseTask)
        }
    }
}

private fun Project.getOrCreateRepoIdTask(
    sonatypeOptions: SonatypeOptions,
): TaskProvider<Task> {
    return try {
        rootProject.tasks.named("createStagingRepo")
    } catch (e: UnknownDomainObjectException) {
        rootProject.tasks.register("createStagingRepo") {
            it.outputs.file(rootProject.layout.buildDirectory.file("stagingRepoId"))

            it.doLast {
                val repoId = runBlocking {
                    nexusStatingClient(sonatypeOptions).createRepository(
                        profileId = sonatypeOptions.stagingProfile,
                        description = "$group:$name:$version"
                    )
                }
                logger.log(LogLevel.LIFECYCLE, "repo created: $repoId")
                it.outputs.files.singleFile.writeText(repoId)
            }
        }
    }
}

fun Project.publishIfNeededTaskProvider(): TaskProvider<Task> {
    check(this == rootProject)
    return try {
        tasks.named("publishIfNeeded")
    } catch (ignored: Exception) {
        tasks.register("publishIfNeeded")
    }
}

fun Project.publishSnapshotsTaskProvider(): TaskProvider<Task> {
    check(this == rootProject)
    return try {
        tasks.named("publishSnapshots")
    } catch (ignored: Exception) {
        tasks.register("publishSnapshots")
    }
}

fun Project.publishStagingTaskProvider(): TaskProvider<Task> {
    check(this == rootProject)
    return try {
        tasks.named("publishStaging")
    } catch (ignored: Exception) {
        tasks.register("publishStaging")
    }
}

private fun nexusStatingClient(sonatypeOptions: SonatypeOptions): NexusStagingClient {
    return NexusStagingClient(
        baseUrl = "${sonatypeOptions.host.toBaseUrl()}/service/local/",
        username = sonatypeOptions.username,
        password = sonatypeOptions.password
    )
}

internal fun Project.getOrCreateRepoId(sonatypeOptions: SonatypeOptions): Provider<String> {
    return getOrCreateRepoIdTask(
        sonatypeOptions,
    ).map {
        it.outputs.files.singleFile.readText()
    }
}

private fun Project.getOrCreateRepoUrl(
    sonatypeOptions: SonatypeOptions
): Provider<String> {
    return getOrCreateRepoId(sonatypeOptions).map {
        "${sonatypeOptions.host.toBaseUrl()}/service/local/staging/deployByRepositoryId/$it/"
    }
}


private fun Project.registerReleaseTask(
    sonatypeOptions: SonatypeOptions,
    autoRelease: Boolean,
    name: String
): TaskProvider<Task> {
    check(this == rootProject)
    val task = try {
        tasks.named(name)
    } catch (e: UnknownDomainObjectException) {
        val repoId = getOrCreateRepoId(sonatypeOptions)
        tasks.register(name) {
            it.inputs.property(
                "repoId",
                repoId
            )
            it.inputs.property(
                "autoRelease",
                autoRelease
            )
            it.doLast {
                runBlocking {
                    val finalizedRepoId = it.inputs.properties.get("repoId") as String
                    val finalizedAutoRelease = it.inputs.properties.get("autoRelease") as Boolean
                    logger.log(LogLevel.LIFECYCLE, "Closing repository $repoId")
                    val nexusStagingClient = nexusStatingClient(sonatypeOptions)
                    nexusStagingClient.closeRepositories(listOf(finalizedRepoId))
                    withTimeout(5.minutes) {
                        nexusStagingClient.waitForClose(finalizedRepoId, 1000) {
                            logger.log(LogLevel.LIFECYCLE, ".")
                        }
                    }
                    if (finalizedAutoRelease) {
                        nexusStagingClient.releaseRepositories(listOf(finalizedRepoId), true)
                    }
                }
            }
        }
    }

    return task
}


fun <T : Task> TaskProvider<T>.dependsOn(other: Any) {
    configure {
        it.dependsOn(other)
    }
}


fun PublicationContainer.createReleasePublication(
    project: Project,
    artifactName: String
) = create("default", MavenPublication::class.java) { publication ->
    apply {
        publication.from(project.components.findByName("java"))

        publication.groupId = project.rootProject.group.toString()
        publication.version = project.rootProject.version.toString()

        publication.pom { pom ->
            pom.name.set(artifactName)
            pom.packaging = "jar"
            pom.description.set(artifactName)
            pom.url.set("https://github.com/gradleup/gratatouille")

            pom.scm {
                it.url.set("https://github.com/gradleup/gratatouille")
                it.connection.set("https://github.com/gradleup/gratatouille")
                it.developerConnection.set("https://github.com/gradleup/gratatouille")
            }

            pom.licenses {
                it.license {
                    it.name.set("MIT License")
                    it.url.set("https://github.com/gradleup/gratatouille/blob/master/LICENSE")
                }
            }

            pom.developers {
                it.developer {
                    it.id.set("GradleUp team")
                    it.name.set("GradleUp team")
                }
            }
        }
    }
}

private fun SonatypeHost.toBaseUrl(): String {
    return when (this) {
        SonatypeHost.OssrhS01 -> "https://s01.oss.sonatype.org"
        SonatypeHost.Ossrh -> "https://oss.sonatype.org"
    }
}

private fun RepositoryHandler.mavenSonatypeSnapshot(
    sonatypeOptions: SonatypeOptions,
) = maven {
    it.name = "ossSnapshots"
    it.url = URI("${sonatypeOptions.host.toBaseUrl()}/content/repositories/snapshots/")
    it.credentials {
        it.username = sonatypeOptions.username
        it.password = sonatypeOptions.password
    }
}

internal fun RepositoryHandler.mavenSonatypeStaging(
    project: Project,
    sonatypeOptions: SonatypeOptions,
) = maven {
    it.name = "ossStaging"
    it.setUrl(project.getOrCreateRepoUrl(sonatypeOptions).map { URI(it) })
    it.credentials {
        it.username = System.getenv(sonatypeOptions.username)
        it.password = System.getenv(sonatypeOptions.password)
    }
}