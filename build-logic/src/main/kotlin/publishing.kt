
import EnvVarKeys.Nexus.username
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.time.Duration.Companion.minutes


object EnvVarKeys {
    object Nexus {
        const val username = "SONATYPE_NEXUS_USERNAME"
        const val password = "SONATYPE_NEXUS_PASSWORD"
        const val profileId = "IO_OPENFEEDBACK_PROFILE_ID"
    }

    object GPG {
        const val privateKey = "OPENFEEDBACK_GPG_PRIVATE_KEY"
        const val password = "OPENFEEDBACK_GPG_PRIVATE_KEY_PASSWORD"
    }

    object GitHub {
        const val event = "GITHUB_EVENT_NAME"
        const val ref = "GITHUB_REF"
    }
}

fun Project.configurePublishing() {
    group = "com.gradleup.gratatouille"

    plugins.apply("maven-publish")
    plugins.apply("signing")
    extensions.configurePublishing(
        project = this@configurePublishing,
        artifactName = name
    )

    extensions.configureSigning()

    tasks.withType(Sign::class.java).configureEach {
        it.isEnabled = !System.getenv(EnvVarKeys.GPG.privateKey).isNullOrBlank()
    }

    rootProject.tasks.named("ossStagingRelease").configure {
        it.dependsOn(tasks.named("publishAllPublicationsToOssStagingRepository"))
    }
}

private fun Project.getOrCreateRepoIdTask(): TaskProvider<Task> {
    return try {
        rootProject.tasks.named("createStagingRepo")
    } catch (e: UnknownDomainObjectException) {
        rootProject.tasks.register("createStagingRepo") {
            it.outputs.file(rootProject.layout.buildDirectory.file("stagingRepoId"))

            it.doLast {
                val repoId = runBlocking {
                    nexusStagingClient.createRepository(
                        profileId = System.getenv(EnvVarKeys.Nexus.profileId),
                        description = "io.openfeedback ${rootProject.version}"
                    )
                }
                logger.log(LogLevel.LIFECYCLE, "repo created: $repoId")
                it.outputs.files.singleFile.writeText(repoId)
            }
        }
    }
}

fun Project.publishIfNeededTaskProvider(): TaskProvider<Task> {
    return try {
        tasks.named("publishIfNeeded")
    } catch (ignored: Exception) {
        tasks.register("publishIfNeeded")
    }
}

private val baseUrl = "https://s01.oss.sonatype.org/service/local/"

private val nexusStagingClient by lazy {
    NexusStagingClient(
        baseUrl = baseUrl,
        username = System.getenv(username)
            ?: error("please set the $username environment variable"),
        password = System.getenv(EnvVarKeys.Nexus.password)
            ?: error("please set the ${EnvVarKeys.Nexus.password} environment variable"),
    )
}

fun Project.getOrCreateRepoId(): Provider<String> {
    return getOrCreateRepoIdTask().map {
        it.outputs.files.singleFile.readText()
    }
}

fun Project.getOrCreateRepoUrl(): Provider<String> {
    return getOrCreateRepoId().map { "${baseUrl}staging/deployByRepositoryId/$it/" }
}

fun Task.closeAndReleaseStagingRepository(repoId: String) {
    runBlocking {
        logger.log(LogLevel.LIFECYCLE, "Closing repository $repoId")
        nexusStagingClient.closeRepositories(listOf(repoId))
        withTimeout(5.minutes) {
            nexusStagingClient.waitForClose(repoId, 1000) {
                logger.log(LogLevel.LIFECYCLE, ".")
            }
        }
        nexusStagingClient.releaseRepositories(listOf(repoId), true)
    }
}

private fun Project.registerReleaseTask(name: String): TaskProvider<Task> {
    val task = try {
        rootProject.tasks.named(name)
    } catch (e: UnknownDomainObjectException) {
        val repoId = getOrCreateRepoId()
        rootProject.tasks.register(name) {
            it.inputs.property(
                "repoId",
                repoId
            )
            it.doLast {
                it.closeAndReleaseStagingRepository(it.inputs.properties.get("repoId") as String)
            }
        }
    }

    return task
}

fun Project.configureRoot() {
    check(this == rootProject) {
        "configureRoot must be called from the root project"
    }

    val publishIfNeeded = project.publishIfNeededTaskProvider()
    val ossStagingReleaseTask = project.registerReleaseTask("ossStagingRelease")

    val eventName = System.getenv(EnvVarKeys.GitHub.event)
    val ref = System.getenv(EnvVarKeys.GitHub.ref)

    if (eventName == "push" && ref == "refs/heads/main" && project.version.toString().endsWith("SNAPSHOT")) {
        project.logger.log(LogLevel.LIFECYCLE, "Deploying snapshot to OssSnapshot...")
        publishIfNeeded.dependsOn(project.tasks.named("publishAllPublicationsToOssSnapshotsRepository"))
    }

    if (ref?.startsWith("refs/tags/") == true) {
        project.logger.log(LogLevel.LIFECYCLE, "Deploying release to OssStaging...")
        publishIfNeeded.dependsOn(ossStagingReleaseTask)
    }
}

fun <T: Task> TaskProvider<T>.dependsOn(other: Any) {
    configure {
        it.dependsOn(other)
    }
}

fun ExtensionContainer.configurePublishing(
    project: Project,
    artifactName: String
) = getByType(PublishingExtension::class.java).apply {
    publications {
        it.createReleasePublication(
            project = project,
            artifactName = artifactName
        )
    }

    repositories {
        it.mavenSonatypeSnapshot(project = project)
        it.mavenSonatypeStaging(project = project)
    }
}

fun ExtensionContainer.configureSigning() = configure(SigningExtension::class.java) {
    // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
    // It can be obtained with gpg --armour --export-secret-keys KEY_ID
    it.useInMemoryPgpKeys(
        System.getenv(EnvVarKeys.GPG.privateKey),
        System.getenv(EnvVarKeys.GPG.password)
    )
    it.sign((getByName("publishing") as PublishingExtension).publications)
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

fun RepositoryHandler.mavenSonatypeSnapshot(project: Project) = maven {
    it.name = "ossSnapshots"
    it.url = project.uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    it.credentials {
        it.username = System.getenv(username)
        it.password = System.getenv(EnvVarKeys.Nexus.password)
    }
}

fun RepositoryHandler.mavenSonatypeStaging(project: Project) = maven {
    it.name = "ossStaging"
    it.setUrl {
        project.uri(project.getOrCreateRepoUrl())
    }
    it.credentials {
        it.username = System.getenv(username)
        it.password = System.getenv(EnvVarKeys.Nexus.password)
    }
}