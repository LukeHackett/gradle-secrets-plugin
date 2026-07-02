package com.lukehackett.gradle.secrets

import com.lukehackett.gradle.secrets.extension.GradleSecretsSettingsExtension
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionContainer
import org.junit.jupiter.api.Test

/**
 * Unit-level tests for [GradleSecretsSettingsPlugin]. End-to-end behaviour (extension registration, per-project
 * forwarding of settings-level configuration, `beforeProject` wiring, etc.) is covered comprehensively by
 * [GradleSecretsSettingsPluginIntegrationTest]. This class holds cheap contract-level checks that do not require
 * spinning up a Gradle build.
 */
class GradleSecretsSettingsPluginTest {
  private val rootDir = File("/tmp/gradle-secrets-plugin-test")

  @Test
  fun `plugin implements Plugin of Settings`() {
    val plugin: Plugin<Settings> = GradleSecretsSettingsPlugin()
    assertNotNull(plugin, "GradleSecretsSettingsPlugin must be constructable")
    assertTrue(plugin is Plugin<*>, "GradleSecretsSettingsPlugin must implement Plugin<Settings>")
  }

  @Test
  fun `apply completes without error against a mocked Settings`() {
    // Given
    val realExtension = GradleSecretsSettingsExtension(rootDir)
    val extensions = mockk<ExtensionContainer>(relaxed = true)
    val gradle = mockk<Gradle>(relaxed = true)
    val settings = mockk<Settings>(relaxed = true)
    every { settings.extensions } returns extensions
    every { settings.gradle } returns gradle
    every { settings.rootDir } returns rootDir
    // Return a real extension so the plugin's assignment succeeds. Use loose matchers because
    // Gradle's `ExtensionContainer.create(...)` uses a Java `Object...` vararg that MockK's
    // structural equality does not match cleanly against a Kotlin call site.
    every { extensions.create(any<String>(), any<Class<Any>>(), *anyVararg<Any>()) } returns realExtension

    // When / Then — should not throw. This exercises the full `apply()` body: extension registration
    // and the `beforeProject { ... }` hook wiring.
    GradleSecretsSettingsPlugin().apply(settings)
  }
}
