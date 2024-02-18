import org.gradle.api.Project

val sonatypeOptions = System.getenv("OSSRH_USER")?.let {
    SonatypeOptions(
        username = it,
        password = System.getenv("OSSRH_PASSWORD") ?: error("OSSRH_PASSWORD not found"),
        host = SonatypeHost.Ossrh,
        stagingProfile = System.getenv("COM_GRADLEUP_PROFILE_ID") ?: error("COM_GRADLEUP_PROFILE_ID not found"),
    )
}

fun Project.configureLib() {
    targetJdk(11)

    configurePublishing(
        projectOptions = ProjectOptions(
            groupId = "com.gradleup.gratatouille",
            version = "0.0.1",
            descriptions = "Cook yourself delicious Gradle plugins",
            vcsUrl = "https://github.com/GradleUp/gratatouille",
            developers = "GradleUp authors",
            license = "MIT License",
            licenseUrl = "https://github.com/GradleUp/gratatouille/blob/main/LICENSE"
        ),
        sonatypeOptions = sonatypeOptions,
        signingOptions = System.getenv("GPG_KEY")?.let {
            SigningOptions(
                privateKey = it,
                privateKeyPassword = System.getenv("GPG_KEY_PASSWORD") ?: error("GPG_KEY_PASSWORD not found")
            )
        },
    )
}

fun Project.configureRoot() {
    configureGitHub(
        sonatypeOptions = sonatypeOptions,
        githubOptions = GithubOptions(
            mainBranch = "main",
            autoRelease = false
        )
    )
}
