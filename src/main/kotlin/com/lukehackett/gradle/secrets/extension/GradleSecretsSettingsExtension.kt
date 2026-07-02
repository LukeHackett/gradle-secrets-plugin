package com.lukehackett.gradle.secrets.extension

import java.io.File

/**
 * Extension registered on the [org.gradle.api.initialization.Settings] object when the
 * `com.lukehackett.gradle.secrets.settings` plugin is applied. It exposes the same DSL as [GradleSecretsExtension] so
 * that shared secret-source configuration can be declared once in `settings.gradle(.kts)` and automatically forwarded
 * to every project in the build.
 *
 * Paths supplied to [file] are resolved relative to the root project directory (i.e. the directory that contains
 * `settings.gradle(.kts)`), which is the conventional location for shared configuration files in a multi-module build.
 *
 * ### Example Usage in settings.gradle.kts:
 * ```kotlin
 * plugins {
 *   id("com.lukehackett.gradle.secrets.settings") version "x.y.z"
 * }
 *
 * secrets {
 *   // Path is resolved relative to the root project directory
 *   file("config/shared.properties")
 *
 *   // Optional: disable sources for every project in the build
 *   disableEnvironment()
 *   disableGradleProperties()
 * }
 * ```
 *
 * Per-project [GradleSecretsExtension] `secrets { ... }` blocks can still add more files or call disable methods on top
 * of the settings-level defaults. Disable flags set at the settings level always win — a per-project block cannot
 * re-enable a source that was disabled at settings level.
 *
 * @param rootDir The root project directory, injected by the settings plugin at registration time.
 */
open class GradleSecretsSettingsExtension(private val rootDir: File) {
  private val customFiles = mutableListOf<File>()
  private var useEnvEnabled = true
  private var useGradleEnabled = true

  /**
   * Adds a file to the list of secret sources that will be forwarded to every project. Later files in the list will
   * override values from earlier files, following the same precedence rules as [GradleSecretsExtension.file].
   *
   * @param path A path relative to the root project directory (e.g., "config/shared.properties").
   */
  fun file(path: Any) {
    customFiles.add(rootDir.resolve(path.toString()))
  }

  /** Disables resolution from System Environment Variables for every project in the build. */
  fun disableEnvironment() {
    useEnvEnabled = false
  }

  /** Disables resolution from Gradle Project properties for every project in the build. */
  fun disableGradleProperties() {
    useGradleEnabled = false
  }

  /** Returns the list of already-resolved absolute secret files declared at the settings level. */
  internal val resolvedFiles: List<File>
    get() = customFiles.toList()

  /** Returns `false` if [disableEnvironment] was called, `true` otherwise. */
  internal val isEnvEnabled: Boolean
    get() = useEnvEnabled

  /** Returns `false` if [disableGradleProperties] was called, `true` otherwise. */
  internal val isGradleEnabled: Boolean
    get() = useGradleEnabled
}
