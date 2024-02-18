
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

fun Project.targetJdk(jdk: Int) {
    tasks.withType(JavaCompile::class.java) {
        it.options.release.set(jdk)
    }

    tasks.withType(KotlinCompile::class.java) {
        it.kotlinOptions.jvmTarget = jdk.toString()
    }
}