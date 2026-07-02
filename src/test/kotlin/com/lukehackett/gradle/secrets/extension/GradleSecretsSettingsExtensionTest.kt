package com.lukehackett.gradle.secrets.extension

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GradleSecretsSettingsExtensionTest {
  @TempDir lateinit var rootDir: File

  private lateinit var extension: GradleSecretsSettingsExtension

  @BeforeEach
  fun setup() {
    extension = GradleSecretsSettingsExtension(rootDir)
  }

  @Test
  fun `file() resolves path relative to rootDir`() {
    // Given
    val relativePath = "config/shared.properties"

    // When
    extension.file(relativePath)

    // Then
    val expected = rootDir.resolve(relativePath).absolutePath
    assertEquals(1, extension.resolvedFiles.size)
    assertEquals(expected, extension.resolvedFiles.first().absolutePath)
  }

  @Test
  fun `file() accepts an already-absolute path`() {
    // Given
    val absoluteFile = File(rootDir, "secrets.properties").also { it.createNewFile() }

    // When
    extension.file(absoluteFile.absolutePath)

    // Then
    assertEquals(1, extension.resolvedFiles.size)
    assertEquals(absoluteFile.absolutePath, extension.resolvedFiles.first().absolutePath)
  }

  @Test
  fun `file() can be called multiple times and preserves registration order`() {
    // Given / When
    extension.file("first.properties")
    extension.file("second.properties")
    extension.file("third.properties")

    // Then
    assertEquals(3, extension.resolvedFiles.size)
    assertTrue(extension.resolvedFiles[0].path.endsWith("first.properties"))
    assertTrue(extension.resolvedFiles[1].path.endsWith("second.properties"))
    assertTrue(extension.resolvedFiles[2].path.endsWith("third.properties"))
  }

  @Test
  fun `isEnvEnabled defaults to true`() {
    assertTrue(extension.isEnvEnabled)
  }

  @Test
  fun `isGradleEnabled defaults to true`() {
    assertTrue(extension.isGradleEnabled)
  }

  @Test
  fun `disableEnvironment() sets isEnvEnabled to false`() {
    // When
    extension.disableEnvironment()

    // Then
    assertFalse(extension.isEnvEnabled)
  }

  @Test
  fun `disableGradleProperties() sets isGradleEnabled to false`() {
    // When
    extension.disableGradleProperties()

    // Then
    assertFalse(extension.isGradleEnabled)
  }

  @Test
  fun `resolvedFiles returns a snapshot and is not affected by subsequent file() calls`() {
    // Given
    extension.file("first.properties")
    val snapshotBefore = extension.resolvedFiles

    // When
    extension.file("second.properties")
    val snapshotAfter = extension.resolvedFiles

    // Then — snapshotBefore is a copy taken before the second call, so it still has 1 entry
    assertEquals(1, snapshotBefore.size)
    assertEquals(2, snapshotAfter.size)
  }
}
