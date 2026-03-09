package com.lukehackett.gradle.secrets

import com.lukehackett.gradle.secrets.extension.GradleSecretsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A Gradle plugin that provides a centralized mechanism for managing sensitive configuration.
 *
 * This plugin registers the `secrets` extension, allowing build scripts to retrieve credentials and tokens using a
 * robust fallback hierarchy (ENV -> File -> Gradle Properties).
 *
 * Usage in build.gradle.kts:
 * ```
 * plugins {
 *     id("com.github.lukehackett.gradle.secrets")
 * }
 *
 * val apiToken = secrets.get("api.token")
 * ```
 *
 * This extension can be configured directly in the `build.gradle.kts` file:
 * ```
 * secrets {
 *     // Override the default location of the secrets file (defaults to secrets.properties)
 *     secretsFile.set(layout.projectDirectory.file("config/auth.properties"))
 * }
 * ```
 */
class GradleSecretsPlugin : Plugin<Project> {
  /**
   * Applies the plugin to the specified [Project].
   *
   * This method registers the [GradleSecretsExtension] under the name "secrets", enabling the configuration block and
   * retrieval methods within the project.
   *
   * Gradle project properties are snapshotted eagerly at configuration time so that the extension does not need to
   * retain a reference to the [Project] object, keeping it compatible with Gradle's configuration cache.
   *
   * @param project The project to which the plugin is being applied.
   */
  override fun apply(project: Project) {
    val extension = project.extensions.create("secrets", GradleSecretsExtension::class.java)

    // Snapshot the Gradle project properties at configuration time so that no Project reference
    // leaks into the serializable extension graph.
    @Suppress("UNCHECKED_CAST")
    extension.gradleProperties = project.properties.filterValues { it is String } as Map<String, String>
  }
}
