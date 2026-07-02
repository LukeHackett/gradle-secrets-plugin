package com.lukehackett.gradle.secrets

import com.lukehackett.gradle.secrets.extension.GradleSecretsExtension
import com.lukehackett.gradle.secrets.extension.GradleSecretsSettingsExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * A Gradle settings plugin that applies the secrets plugin to every project in a multi-module build.
 *
 * Applying this plugin in `settings.gradle(.kts)` is the recommended approach when you need the `secrets` extension
 * available across all projects (i.e. inside `allprojects { }` or `subprojects { }` blocks in the root build script, or
 * directly in each subproject's own build script).
 *
 * This plugin also registers a `secrets` DSL block at the settings level so that shared secret-source configuration
 * (secret files and source toggles) can be declared once and automatically forwarded to every project as defaults.
 * Per-project `secrets { ... }` blocks in individual build scripts can add further sources on top of these defaults.
 *
 * ### Example Usage in settings.gradle.kts:
 * ```kotlin
 * plugins {
 *   id("com.lukehackett.gradle.secrets.settings") version "x.y.z"
 * }
 *
 * secrets {
 *   // Resolved relative to settings.rootDir; forwarded to every project as a default source
 *   file("config/shared.properties")
 *
 *   // Optionally disable sources for every project in the build
 *   disableEnvironment()
 *   disableGradleProperties()
 * }
 * ```
 *
 * After applying this plugin, the `secrets` extension is available in every project — including inside `allprojects {
 * }` and `subprojects { }` blocks — without any additional `apply` calls in build scripts.
 *
 * ### Single-module builds
 * If you have a single-module build, use the project plugin instead:
 * ```kotlin
 * plugins {
 *   id("com.lukehackett.gradle.secrets") version "x.y.z"
 * }
 * ```
 *
 * @see GradleSecretsPlugin
 */
class GradleSecretsSettingsPlugin : Plugin<Settings> {
  /**
   * Applies the plugin to the given [Settings] instance.
   *
   * Registers the [GradleSecretsSettingsExtension] under the name "secrets" on the [Settings] object, and configures a
   * [org.gradle.api.invocation.Gradle.beforeProject] callback that:
   * 1. Applies the [GradleSecretsPlugin] to every project (idempotent — safe if also applied manually).
   * 2. Forwards the settings-level file sources and source-disable flags as defaults into each project's
   *    [GradleSecretsExtension] before the project's own build script evaluates.
   *
   * @param settings The [Settings] instance to which the plugin is being applied.
   */
  override fun apply(settings: Settings) {
    val settingsExtension =
        settings.extensions.create(
            "secrets",
            GradleSecretsSettingsExtension::class.java,
            settings.rootDir,
        )

    settings.gradle.beforeProject(
        Action {
          // Apply the project plugin (idempotent: Gradle skips double-application automatically)
          pluginManager.apply(GradleSecretsPlugin::class.java)

          val projectExtension = extensions.getByType(GradleSecretsExtension::class.java)

          // Forward settings-level file sources. Paths are already absolute (resolved against rootDir
          // at settings configuration time), so the project extension receives them unchanged.
          settingsExtension.resolvedFiles.forEach { file -> projectExtension.file(file.absolutePath) }

          // Forward disable flags. Settings-level disables always win: if a source is disabled at
          // settings level it is disabled for every project, regardless of per-project configuration.
          if (!settingsExtension.isEnvEnabled) {
            projectExtension.disableEnvironment()
          }
          if (!settingsExtension.isGradleEnabled) {
            projectExtension.disableGradleProperties()
          }
        },
    )
  }
}
