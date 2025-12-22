import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
  val projectDir = File("build/testProject")

  @Test
  fun test() {
    projectDir.deleteRecursively()

    File("testProjects/simple").copyRecursively(projectDir)

    build("help").apply {
      assertEquals(TaskOutcome.SUCCESS, task(":help")!!.outcome)
    }
  }

  private fun build(task: String): BuildResult {
    return GradleRunner.create()
      .withDebug(true)
      .withProjectDir(projectDir)
      .withArguments(task)
      .build()!!
  }
}