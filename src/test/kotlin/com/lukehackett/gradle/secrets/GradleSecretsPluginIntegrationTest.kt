package com.lukehackett.gradle.secrets

import java.io.File
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GradleSecretsPluginIntegrationTest {
  @TempDir lateinit var testProjectDir: File

  // Helper to prepare the "Given" state
  private fun givenProject(resourcePath: String) {
    val resourceUri =
        javaClass.classLoader.getResource(resourcePath)?.toURI()
            ?: throw IllegalArgumentException("Could not find resource: $resourcePath")
    File(resourceUri).copyRecursively(testProjectDir, overwrite = true)
  }

  // Helper to handle the "When" state (standardizes the Runner setup)
  private fun whenRunningGradle(
      args: List<String> = listOf("printSecret", "-q"),
      env: Map<String, String> = mapOf(), // Default to prevent missing key errors
      expectFailure: Boolean = false,
  ) =
      GradleRunner.create()
          .withProjectDir(testProjectDir)
          .withArguments(args)
          .withPluginClasspath()
          .withEnvironment(env)
          .let { if (expectFailure) it.buildAndFail() else it.build() }

  @Test
  fun `should resolve secret from Environment Variable`() {
    // Given
    givenProject("functionalTest/basic-case")
    val envVars = mapOf("MY_API_KEY" to "env-secret-123")

    // When
    val result = whenRunningGradle(env = envVars)

    // Then
    assertTrue(result.output.contains("SECRET_VALUE=env-secret-123"))
  }

  @Test
  fun `should resolve from explicitly registered file`() {
    // Given
    givenProject("functionalTest/with-file-registration")
    File(testProjectDir, "secrets.properties").writeText("my.api.key=file-value-456")

    // When
    val result = whenRunningGradle()

    // Then
    assertTrue(result.output.contains("SECRET_VALUE=file-value-456"))
  }

  @Test
  fun `should support type conversion in actual build`() {
    // Given
    givenProject("functionalTest/type-conversion")
    val args = listOf("printPort", "-q", "-Pserver.port=8080")

    // When
    val result = whenRunningGradle(args = args, env = emptyMap())

    // Then
    assertTrue(result.output.contains("PORT_TYPE=Int"), "Output should indicate Integer type")
    assertTrue(result.output.contains("PORT_VALUE=8080"), "Output should contain the port value")
  }

  @Test
  fun `should throw error when secret is missing`() {
    // Given
    givenProject("functionalTest/basic-case")
    // We use a key that doesn't exist in ENV, Files, or Properties
    val args = listOf("printSecret", "-q", "-PtargetKey=non.existent.key")

    // When
    val result = whenRunningGradle(args = args, expectFailure = true)

    // Then
    assertTrue(result.output.contains("Secret 'non.existent.key' not found"))
  }

  @Test
  fun `hierarchy - ENV should override registered file`() {
    // Given
    givenProject("functionalTest/with-file-registration")
    File(testProjectDir, "secrets.properties").writeText("my.api.key=file-value")
    val envVars = mapOf("MY_API_KEY" to "env-wins")

    // When
    val result = whenRunningGradle(env = envVars)

    // Then
    assertTrue(result.output.contains("SECRET_VALUE=env-wins"))
  }

  @Test
  fun `hierarchy - registered file should override gradle properties`() {
    // Given
    givenProject("functionalTest/with-file-registration")
    File(testProjectDir, "secrets.properties").writeText("my.api.key=file-wins")
    val args = listOf("printSecret", "-q", "-Pmy.api.key=gradle-prop-value")

    // When
    val result = whenRunningGradle(args = args)

    // Then
    assertTrue(result.output.contains("SECRET_VALUE=file-wins"))
  }
}
