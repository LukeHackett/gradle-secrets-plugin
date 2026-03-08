<!-- Project Introduction -->
<div align="center">
  <a href="https://github.com/LukeHackett/gradle-secrets-plugin">
    <img src="./.github/docs/logo.png" alt="Logo" width="390" height="109">
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
  </p>
</div>

<!-- Core Features -->
## Features

- 🔐 **Unified Secret Resolution** — Retrieve secrets from environment variables, files, and Gradle properties through a single API
- 📋 **Strict Priority Hierarchy** — Sources are resolved in a deterministic order: Environment Variables → Secret Files → Gradle Properties
- 🔒 **Type-Safe Access** — Decode secrets as `String`, `Int`, `Long`, `Boolean`, `Float`, `Double`, or custom types
- 📁 **Multiple File Sources** — Register multiple secret files (`.properties`, `.yaml`, `.toml`, `.json`), with later files overriding earlier ones
- ⚙️ **Configurable Sources** — Selectively disable environment variables or Gradle properties as sources
- 🛡️ **Default Values** — Provide fallback defaults to avoid build failures when a secret is missing
- 🧩 **Kotlin & Groovy Compatible** — Works seamlessly in both `build.gradle.kts` and `build.gradle` scripts

<!-- Getting Started -->
## Installation

Apply the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.lukehackett.gradle.secrets") version "1.0.0"
}
```

<details>
<summary>Using Groovy (build.gradle)</summary>

```groovy
plugins {
    id 'com.lukehackett.gradle.secrets' version '1.0.0'
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
        classpath("com.lukehackett.gradle.secrets:gradle-secrets-plugin:1.0.0")
    }
}

apply(plugin = "com.lukehackett.gradle.secrets")
```

</details>

<!-- Usage -->
## Usage

Once the plugin is applied, a `secrets` extension is automatically available in your build script. 

Use it to retrieve sensitive values such as API keys, tokens, and passwords.

### Basic Usage

```kotlin
// Retrieve a secret as a String
val apiKey: String = secrets.asString("my.api.key")

// Use the type-safe generic accessor (Kotlin only)
val apiKey: String = secrets.get("my.api.key")
```

### Type Conversions

The plugin provides type-safe accessors for common types:

```kotlin
val host: String  = secrets.asString("server.host")
val port: Int     = secrets.asInt("server.port")
val timeout: Long = secrets.asLong("server.timeout")
val debug: Boolean = secrets.asBoolean("app.debug")
val rate: Float   = secrets.asFloat("app.rate")
val pi: Double    = secrets.asDouble("math.pi")
```

### Default Values

Provide a fallback value to avoid build failures when a secret is not found:

```kotlin
val port: Int = secrets.asInt("server.port", 8080)
val debug: Boolean = secrets.asBoolean("app.debug", false)
```

### Secret Resolution Order

Secrets are resolved using the following priority (highest to lowest):

| Priority | Source                  | Example                                           |
|----------|-------------------------|----------------------------------------------------|
| 1st      | Environment Variables   | `MY_API_KEY=secret123`                             |
| 2nd      | Registered Secret Files | `my.api.key=secret123` in `secrets.properties`     |
| 3rd      | Gradle Properties       | `-Pmy.api.key=secret123` or in `gradle.properties` |

Environment variable names are matched by converting the dot-notation key to uppercase with underscores (e.g., `my.api.key` → `MY_API_KEY`).

### Using Secret Files

Register one or more secret files. Supported formats include `.properties`, `.yaml`, `.toml`, and `.json`. Later files take priority over earlier ones:

```kotlin
secrets {
    file("config/base.properties")
    file("config/local.properties") // values here override base.properties
}
```

### Configuration

The `secrets` extension supports the following configuration options:

| Option                   | Description                                                      | Default   |
|--------------------------|------------------------------------------------------------------|-----------|
| `file(path)`             | Registers a secret file source. Can be called multiple times.    | _none_    |
| `disableEnvironment()`   | Disables resolution from system environment variables.           | _enabled_ |
| `disableGradleProperties()` | Disables resolution from Gradle project properties.          | _enabled_ |

#### Full Configuration Example

```kotlin
plugins {
    id("com.lukehackett.gradle.secrets") version "1.0.0"
}

secrets {
    // Register secret files (later files override earlier ones)
    file("config/base.properties")
    file("config/local.properties")

    // Optionally disable specific sources
    disableEnvironment()
    disableGradleProperties()
}

tasks.register("deploy") {
    doLast {
        val token = secrets.asString("deploy.token")
        val timeout = secrets.asInt("deploy.timeout", 30000)
        println("Deploying with token and timeout=$timeout")
    }
}
```

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

Please review the [Contributing Guidelines](./CONTRIBUTING.md) if you would like to make a contribution to this project.

## Issues

If you encounter any other bugs or need some other features feel free to open an [issue](https://github.com/LukeHackett/gradle-secrets-plugin/issues).

## License

Distributed under the MIT License. See [LICENSE.md](./LICENSE) for more information.
