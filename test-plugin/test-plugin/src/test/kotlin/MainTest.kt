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

    build("taskAction1").apply {
      assertEquals(TaskOutcome.SUCCESS, task(":taskAction1")!!.outcome)
    }
    build("taskAction2").apply {
      assertEquals(TaskOutcome.UP_TO_DATE, task(":taskAction1")!!.outcome)
      assertEquals(TaskOutcome.SUCCESS, task(":taskAction2")!!.outcome)
    }
    build("taskAction3").apply {
      assertEquals(TaskOutcome.SUCCESS, task(":taskAction3")!!.outcome)
    }
    build("taskAction4").apply {
      assertEquals(TaskOutcome.SUCCESS, task(":taskAction4")!!.outcome)
    }
    build("taskAction4").apply {
      // taskAction4 is impure and never up-to-date
      assertEquals(TaskOutcome.SUCCESS, task(":taskAction4")!!.outcome)
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