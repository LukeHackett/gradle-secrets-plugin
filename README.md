<!-- Project Introduction -->
<div align="center">
  <a href="https://github.com/LukeHackett/gradle-secrets-plugin">
    <img src="./.github/docs/logo.png" alt="Logo" width="400" height="125">
  </a>
  <h3 align="center">gradle-secrets-plugin</h3>
  <p align="center">
    A type-safe <a href="https://gradle.org/">gradle</a> plugin for retrieving sensitive configuration from 
    environment variables, files, and Gradle properties
  </p>
  <p align="center">
    <img src="https://img.shields.io/github/stars/LukeHackett/gradle-secrets-plugin?label=GitHub%20Stars&style=flat-square" alt="GitHub Repository Stars" />
    <a href="https://github.com/LukeHackett/gradle-secrets-plugin/blob/main/LICENSE">
      <img src="https://img.shields.io/github/license/LukeHackett/gradle-secrets-plugin?style=flat-square" alt="License" />
    </a>
    <img src="https://img.shields.io/github/last-commit/LukeHackett/gradle-secrets-plugin?style=flat-square" alt="Last Commit" />
    <img src="https://img.shields.io/github/actions/workflow/status/LukeHackett/gradle-secrets-plugin/build.yml?branch=main&style=flat-square" alt="Build Status" />
    <a href="https://github.com/LukeHackett/gradle-secrets-plugin/issues">
      <img src="https://img.shields.io/github/issues/LukeHackett/gradle-secrets-plugin?style=flat-square" alt="Issues" />
    </a>
    <a href="https://plugins.gradle.org/plugin/com.lukehackett.gradle.secrets">
      <img src="https://img.shields.io/gradle-plugin-portal/v/com.lukehackett.gradle.secrets?style=flat-square" alt="Latest Version" />
    </a>
  </p>
</div>

<!-- Core Features -->
## Features

- 🔐 **[Unified Secret Resolution](./docs/usage.md#basic-usage)** — Retrieve secrets from environment variables, files, and Gradle properties through a single API
- 📋 **[Strict Priority Hierarchy](./docs/usage.md#secret-resolution-order)** — Sources are resolved in a deterministic order: Environment Variables → Secret Files → Gradle Properties
- 🔒 **[Type-Safe Access](./docs/usage.md#type-conversions)** — Decode secrets as `String`, `Int`, `Long`, `Boolean`, `Float`, `Double`, or custom types
- 📁 **[Multiple File Sources](./docs/usage.md#using-secret-files)** — Register multiple secret files (`.properties`, `.yaml`, `.toml`, `.json`), with later files overriding earlier ones
- ⚙️ **[Configurable Sources](./docs/usage.md#configuration)** — Selectively disable environment variables or Gradle properties as sources
- 🛡️ **[Default Values](./docs/usage.md#default-values)** — Provide fallback defaults to avoid build failures when a secret is missing
- 🧩 **Kotlin & Groovy Compatible** — Works seamlessly in both `build.gradle.kts` and `build.gradle` scripts
- 🔗 **[Multi-module Ready](./docs/multi-module.md)** — A settings-level plugin variant applies the extension to every project automatically

<!-- Requirements -->
## Requirements

| Requirement | Version |
|---|---|
| Gradle | 9.0 or later |
| JVM | 25 or later |
| Configuration cache | ✅ Supported |
| Kotlin DSL / Groovy DSL | ✅ Both |

<!-- Getting Started -->
## Installation

Two plugin IDs are published from the same artifact — pick the one that matches your build layout:

| Plugin ID | Apply in | Use for |
|---|---|---|
| `com.lukehackett.gradle.secrets` | `build.gradle(.kts)` | Single-module builds, or multi-module builds where you apply it explicitly per project |
| `com.lukehackett.gradle.secrets.settings` | `settings.gradle(.kts)` | Multi-module builds — applies the plugin to every project automatically, with optional shared configuration |

### Single-module builds

```kotlin
// build.gradle.kts
plugins {
    id("com.lukehackett.gradle.secrets") version "<latest>"
}
```

### Multi-module builds

```kotlin
// settings.gradle.kts
plugins {
    id("com.lukehackett.gradle.secrets.settings") version "<latest>"
}
```

<details>
<summary>Using Groovy (build.gradle / settings.gradle)</summary>

```groovy
// build.gradle
plugins {
    id 'com.lukehackett.gradle.secrets' version '<latest>'
}

// settings.gradle
plugins {
    id 'com.lukehackett.gradle.secrets.settings' version '<latest>'
}
```

</details>

<details>
<summary>Using Legacy Plugin Application</summary>

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.lukehackett.gradle.secrets:gradle-secrets-plugin:<latest>")
    }
}

apply(plugin = "com.lukehackett.gradle.secrets")
```

</details>

<!-- Quick Start -->
## Quick Start

Once the plugin is applied, the `secrets` extension is available in your build scripts.

### Single-module build

```kotlin
// build.gradle.kts
plugins {
    id("com.lukehackett.gradle.secrets") version "<latest>"
}

secrets {
    file("config/secrets.properties")
}

tasks.register("deploy") {
    doLast {
        val apiKey = secrets.asString("my.api.key")
        println("Deploying with key length=${apiKey.length}")
    }
}
```

### Multi-module build

Apply the settings plugin once, then use `secrets` in any project — including inside `allprojects { }` / `subprojects { }`:

```kotlin
// settings.gradle.kts
plugins {
    id("com.lukehackett.gradle.secrets.settings") version "<latest>"
}

// Optional: shared secret sources applied as defaults to every project
secrets {
    file("config/shared.properties")
}
```

```kotlin
// any build.gradle.kts (root or subproject)
tasks.register("deploy") {
    doLast {
        val apiKey = secrets.asString("my.api.key")
        println("Deploying with key length=${apiKey.length}")
    }
}
```

<!-- Documentation -->
## Documentation

| Guide | Contents |
|---|---|
| [Usage Guide](./docs/usage.md) | Retrieving secrets, type conversions, defaults, resolution order, file sources, configuration options |
| [Multi-module Builds](./docs/multi-module.md) | Settings plugin, shared configuration, per-project overrides, version catalogs, fallback patterns |

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

Please review the [Contributing Guidelines](./CONTRIBUTING.md) if you would like to make a contribution to this project.

## Issues

If you encounter any other bugs or need some other features feel free to open an [issue](https://github.com/LukeHackett/gradle-secrets-plugin/issues).

## License

Distributed under the MIT License. See [LICENSE.md](./LICENSE) for more information.
