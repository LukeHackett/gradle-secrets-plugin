package com.lukehackett.gradle.secrets.extension

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GradleSecretsExtensionTest {
  @TempDir lateinit var tempDir: File

  private lateinit var project: Project
  private lateinit var extension: GradleSecretsExtension

  @BeforeEach
  fun setup() {
    // Given
    project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    extension = project.objects.newInstance(GradleSecretsExtension::class.java, project)
  }

  @Test
  fun `error message should contain key when not found`() {
    // Given
    val key = "my.secret-key"

    // When
    val exception = assertFailsWith<GradleException> { extension.get<String>(key) }

    // Then
    assertTrue(exception.message!!.contains(key))
    assertTrue(exception.message!!.contains("not found"))
  }

  @Test
  fun `resolves from custom secrets file location`() {
    // Given
    val key = "db.password"
    val value = "custom-value"
    val customFile =
        File(tempDir, "custom/config.props").apply {
          parentFile.mkdirs()
          writeText("$key=$value")
        }
    extension.file(customFile)

    // When
    val result = extension.get<String>(key)

    // Then
    assertEquals(value, result)
  }

  @Test
  fun `resolves from gradle_properties (Project Properties)`() {
    // Given
    val key = "gradleProp"
    val value = "property-value"
    project.extensions.extraProperties.set(key, value)

    // When
    val result = extension.get<String>(key)

    // Then
    assertEquals(value, result)
  }

  @Test
  fun `returns default value if nothing found`() {
    // Given
    val key = "missing.key"
    val defaultValue = "i-am-default"

    // When
    val result = extension.get(key, defaultValue)

    // Then
    assertEquals(defaultValue, result)
  }

  @Test
  fun `generic get should decode types`() {
    // Given
    project.extensions.extraProperties.set("port", "8081")

    // When
    val port: Int = extension.get("port")

    // Then
    assertEquals(8081, port)
  }

  @Test
  fun `should support type conversion to Int`() {
    // Given
    project.extensions.extraProperties.set("server.port", "8080")

    // When
    val port = extension.asInt("server.port")

    // Then
    assertEquals(8080, port)
  }

  @Test
  fun `should support type conversion to Long`() {
    // Given
    project.extensions.extraProperties.set("timeout.long", "9999")

    // When
    val result = extension.asLong("timeout.long")

    // Then
    assertEquals(9999L, result)
  }

  @Test
  fun `should support type conversion to Float`() {
    // Given
    project.extensions.extraProperties.set("pi.float", "3.14")

    // When
    val result = extension.asFloat("pi.float")

    // Then
    assertEquals(3.14f, result)
  }

  @Test
  fun `should support type conversion to Double`() {
    // Given
    project.extensions.extraProperties.set("pi.double", "3.1415")

    // When
    val result = extension.asDouble("pi.double")

    // Then
    assertEquals(3.1415, result)
  }

  @Test
  fun `should support type conversion to Boolean`() {
    // Given
    project.extensions.extraProperties.set("feature.enabled", "true")

    // When
    val enabled = extension.asBoolean("feature.enabled")

    // Then
    assertTrue(enabled)
  }

  @Test
  fun `disabling gradle properties should make key invisible`() {
    // Given
    val key = "test.key"
    project.extensions.extraProperties.set(key, "some-value")
    extension.disableGradleProperties()

    // When
    val action = { extension.get<String>(key) }

    // Then
    assertFailsWith<GradleException> { action() }
  }

  @Test
  fun `multiple files - last registered file wins`() {
    // Given
    val key = "shared.key"
    val file1 = File(tempDir, "file1.props").apply { writeText("$key=value1") }
    val file2 = File(tempDir, "file2.props").apply { writeText("$key=value2") }

    extension.file(file1)
    extension.file(file2)

    // When
    val result = extension.asString(key)

    // Then
    assertEquals("value2", result)
  }

  @Test
  fun `a given secrets_file should override gradle_properties`() {
    // Given
    val key = "overlap.key"
    val fileValue = "value-from-file"
    val gradleValue = "value-from-gradle"

    val secretsFile = File(tempDir, "secrets.properties")
    secretsFile.writeText("$key=$fileValue")

    extension.file(secretsFile)
    project.extensions.extraProperties.set(key, gradleValue)

    // When
    val result = extension.get<String>(key)

    // Then
    assertEquals(fileValue, result)
  }

  @Test
  fun `asType should work for Java-style class access`() {
    // Given
    project.extensions.extraProperties.set("timeout", "100")

    // When
    val result = extension.asType("timeout", Int::class.javaObjectType)

    // Then
    assertEquals(100, result)
  }

  @Test
  fun `should fail if value cannot be decoded to requested type`() {
    project.extensions.extraProperties.set("port", "not-a-number")

    val exception = assertFailsWith<GradleException> { extension.asInt("port") }

    assertTrue(exception.message!!.contains("port"))
    assertTrue(exception.message!!.contains("Int"))
  }

  @Test
  fun `dot notation should resolve nested keys`() {
    // Given
    val file = File(tempDir, "nested.props").apply { writeText("db.password=secret") }

    extension.file(file)

    // When
    val result = extension.get<String>("db.password")

    // Then
    assertEquals("secret", result)
  }
}
