# Multi-module Builds

> **Which plugin should I use?** — Use `com.lukehackett.gradle.secrets.settings` in `settings.gradle(.kts)` for a one-line setup that covers every project. Fall back to `com.lukehackett.gradle.secrets` with `allprojects { apply(...) }` only if you cannot modify `settings.gradle(.kts)`.

## Table of Contents

- [Why the project plugin doesn't cover subprojects](#why-the-project-plugin-doesnt-cover-subprojects)
- [Applying the settings plugin](#applying-the-settings-plugin)
- [Settings-plugin Configuration Options](#settings-plugin-configuration-options)
- [Using `secrets` across projects](#using-secrets-across-projects)
- [Per-project overrides](#per-project-overrides)
- [Version catalogs and settings plugins](#version-catalogs-and-settings-plugins)
- [Fallback: manual application via `allprojects`](#fallback-manual-application-via-allprojects)

## Why the project plugin doesn't cover subprojects

The project plugin (`com.lukehackett.gradle.secrets`) registers the `secrets` extension on each project it is applied to. Applying it only to the root project will **not** make `secrets` available in `allprojects { }` or `subprojects { }` blocks, because Gradle plugin application is not inherited by subprojects.

## Applying the settings plugin

The recommended solution for multi-module builds is to apply the companion settings plugin in `settings.gradle(.kts)`. This automatically applies the project plugin to every project (including the root) before each build script evaluates.

```kotlin
// settings.gradle.kts
plugins {
    id("com.lukehackett.gradle.secrets.settings") version "<latest>"
}

// Optional: declare shared secret sources once — these become the default for every project
secrets {
    // Path is resolved relative to the root project directory
    file("config/shared.properties")

    // Optionally disable sources globally
    // disableEnvironment()
    // disableGradleProperties()
}

rootProject.name = "my-app"
include("api", "web", "core")
```

## Settings-plugin Configuration Options

The settings-level `secrets` extension exposes the same DSL as the project extension, applied as defaults to every project:

| Option                   | Description                                                                              | Default   |
|--------------------------|------------------------------------------------------------------------------------------|-----------|
| `file(path)`             | Registers a shared secret file source. Path is resolved relative to `settings.rootDir`.  | _none_    |
| `disableEnvironment()`   | Disables environment-variable resolution across every project. Cannot be re-enabled per project. | _enabled_ |
| `disableGradleProperties()` | Disables Gradle-property resolution across every project. Cannot be re-enabled per project. | _enabled_ |

## Using `secrets` across projects

After applying the settings plugin, the `secrets` extension is available everywhere:

```kotlin
// root build.gradle.kts
allprojects {
    val apiKey = secrets.asString("my.api.key")
}
```

## Per-project overrides

Per-project `secrets { }` blocks in individual subproject build scripts can still add extra file sources on top of the settings-level defaults:

```kotlin
// api/build.gradle.kts
secrets {
    file("api-local.properties") // adds to (does not replace) the settings-level shared file
}
```

Each project resolves its own Gradle properties independently, so a subproject's `gradle.properties` can override values from the root.

## Version catalogs and settings plugins

The `libs.plugins.xyz` accessor from `libs.versions.toml` is **not** available inside `settings.gradle(.kts)`'s own `plugins { }` block (a Gradle limitation). Use one of these patterns instead:

### Option A — `gradle.properties` (simplest)

```properties
# gradle.properties
secretsPluginVersion=<latest>
```

```kotlin
// settings.gradle.kts
plugins {
    val secretsPluginVersion: String by settings
    id("com.lukehackett.gradle.secrets.settings") version secretsPluginVersion
}
```

### Option B — `pluginManagement.resolutionStrategy` (version declared once, no literal in any script)

```kotlin
// settings.gradle.kts
pluginManagement {
    val secretsPluginVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.lukehackett.gradle.secrets")) {
                useVersion(secretsPluginVersion)
            }
        }
    }
}

plugins {
    id("com.lukehackett.gradle.secrets.settings") // no version needed here
}
```

## Fallback: manual application via `allprojects`

If you cannot modify `settings.gradle(.kts)` (e.g. in a convention-plugin build), you can still apply the plugin manually to every project from the root build script:

```kotlin
// root build.gradle.kts
plugins {
    id("com.lukehackett.gradle.secrets") version "<latest>" apply false
}

allprojects {
    apply(plugin = "com.lukehackett.gradle.secrets")

    val apiKey = secrets.asString("my.api.key")
}
```

<details>
<summary>Using Groovy (build.gradle)</summary>

```groovy
plugins {
    id 'com.lukehackett.gradle.secrets' version '<latest>' apply false
}

allprojects {
    apply plugin: 'com.lukehackett.gradle.secrets'

    def apiKey = secrets.asString('my.api.key')
}
```

</details>

---

[← Back to README](../README.md) · [Usage Guide](./usage.md)
