package com.lukehackett.gradle.secrets

import java.io.File
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GradleSecretsSettingsPluginIntegrationTest {
  @TempDir lateinit var testProjectDir: File

  private fun givenProject(resourcePath: String) {
    val resourceUri =
        javaClass.classLoader.getResource(resourcePath)?.toURI()
            ?: throw IllegalArgumentException("Could not find resource: $resourcePath")
    File(resourceUri).copyRecursively(testProjectDir, overwrite = true)
  }

  private fun whenRunningGradle(
      args: List<String>,
      env: Map<String, String> = mapOf(),
      expectFailure: Boolean = false,
  ) =
      GradleRunner.create()
          .withProjectDir(testProjectDir)
          .withArguments(args)
          .withPluginClasspath()
          .withEnvironment(env)
          .let { if (expectFailure) it.buildAndFail() else it.build() }

  @Test
  fun `should register secrets extension on every project via settings plugin`() {
    // Given
    givenProject("functionalTest/multi-module")
    val args = listOf(":api:printSecret", ":web:printSecret", "-q")

    // When
    val result = whenRunningGradle(args = args)

    // Then — both subprojects resolve the value from the settings-level shared file
    assertTrue(result.output.contains("PROJECT=api SECRET_VALUE=shared-value"))
    assertTrue(result.output.contains("PROJECT=web SECRET_VALUE=shared-value"))
  }

  @Test
  fun `should make secrets available on root project as well`() {
    // Given
    givenProject("functionalTest/multi-module")
    val args = listOf(":printSecret", "-q")

    // When
    val result = whenRunningGradle(args = args)

    // Then — the root project also has the secrets extension and resolves the shared value
    assertTrue(result.output.contains("PROJECT=multi-module-test SECRET_VALUE=shared-value"))
  }

  @Test
  fun `should resolve from environment variable in all projects`() {
    // Given
    givenProject("functionalTest/multi-module")
    val args = listOf(":api:printSecret", ":web:printSecret", "-q")
    val envVars = mapOf("MY_API_KEY" to "env-wins")

    // When
    val result = whenRunningGradle(args = args, env = envVars)

    // Then — env var takes priority over the settings-level shared file
    assertTrue(result.output.contains("PROJECT=api SECRET_VALUE=env-wins"))
    assertTrue(result.output.contains("PROJECT=web SECRET_VALUE=env-wins"))
  }

  @Test
  fun `should forward settings-level disableEnvironment to every project`() {
    // Given
    givenProject("functionalTest/multi-module")
    // Overwrite settings to add disableEnvironment()
    File(testProjectDir, "settings.gradle.kts")
        .writeText(
            """
            plugins { id("com.lukehackett.gradle.secrets.settings") }
            secrets {
                file("config/shared.properties")
                disableEnvironment()
            }
            rootProject.name = "multi-module-test"
            include("api", "web")
            """
                .trimIndent()
        )
    val args = listOf(":api:printSecret", ":web:printSecret", "-q")
    // Even with an env var set, the environment source is disabled
    val envVars = mapOf("MY_API_KEY" to "env-value-should-be-ignored")

    // When
    val result = whenRunningGradle(args = args, env = envVars)

    // Then — both subprojects fall back to the shared file, ignoring the env var
    assertTrue(result.output.contains("PROJECT=api SECRET_VALUE=shared-value"))
    assertTrue(result.output.contains("PROJECT=web SECRET_VALUE=shared-value"))
  }

  @Test
  fun `should preserve per-project gradle properties snapshots`() {
    // Given
    givenProject("functionalTest/multi-module")
    // Use a key that is NOT in the shared properties file so Gradle properties are consulted
    File(testProjectDir, "api/gradle.properties").writeText("per.project.key=api-property-value")
    File(testProjectDir, "web/gradle.properties").writeText("per.project.key=web-property-value")
    val args = listOf(":api:printSecret", ":web:printSecret", "-q", "-PtargetKey=per.project.key")

    // When
    val result = whenRunningGradle(args = args)

    // Then — each subproject sees its own per-project Gradle property value
    assertTrue(result.output.contains("PROJECT=api SECRET_VALUE=api-property-value"))
    assertTrue(result.output.contains("PROJECT=web SECRET_VALUE=web-property-value"))
  }

  @Test
  fun `should let per-project secrets block add file sources on top of settings-level defaults`() {
    // Given
    givenProject("functionalTest/multi-module-overrides")
    // Write the api-local.properties file — this should take priority over the shared file
    // because it is registered later (Hoplite processes later files first)
    File(testProjectDir, "api/api-local.properties").writeText("my.api.key=api-local-override")

    val args = listOf(":api:printSecret", ":web:printSecret", "-q")

    // When
    val result = whenRunningGradle(args = args)

    // Then — api sees its local override; web falls back to the settings-level shared file
    assertTrue(result.output.contains("PROJECT=api SECRET_VALUE=api-local-override"))
    assertTrue(result.output.contains("PROJECT=web SECRET_VALUE=shared-value"))
  }

  @Test
  fun `should be idempotent when project plugin is also applied explicitly`() {
    // Given
    givenProject("functionalTest/multi-module")
    // api explicitly applies the project plugin on top of what the settings plugin already applied
    File(testProjectDir, "api/build.gradle.kts")
        .writeText(
            """
            apply(plugin = "com.lukehackett.gradle.secrets")
            """
                .trimIndent()
        )
    val args = listOf(":api:printSecret", ":web:printSecret", "-q")

    // When / Then — build should not fail; both subprojects resolve correctly
    val result = whenRunningGradle(args = args)
    assertTrue(result.output.contains("PROJECT=api SECRET_VALUE=shared-value"))
    assertTrue(result.output.contains("PROJECT=web SECRET_VALUE=shared-value"))
  }
}
