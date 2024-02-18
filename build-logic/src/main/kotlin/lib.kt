import org.gradle.api.Project

fun Project.configureLib() {
    targetJdk(11)
    configurePublishing()
}
