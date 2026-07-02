# Usage Guide

Once the `com.lukehackett.gradle.secrets` plugin is applied, a `secrets` extension is automatically available in your build script. Use it to retrieve sensitive values such as API keys, tokens, and passwords.

For applying the plugin across a multi-module build, see [Multi-module Builds](./multi-module.md).

## Table of Contents

- [Basic Usage](#basic-usage)
- [Type Conversions](#type-conversions)
  - [Custom Types (Java & Groovy)](#custom-types-java--groovy)
- [Default Values](#default-values)
- [Secret Resolution Order](#secret-resolution-order)
- [Using Secret Files](#using-secret-files)
- [Configuration](#configuration)
  - [Full Configuration Example](#full-configuration-example)

## Basic Usage

```kotlin
// Retrieve a secret as a String
val apiKey: String = secrets.asString("my.api.key")

// Use the type-safe generic accessor (Kotlin only)
val apiKey: String = secrets.get("my.api.key")
```

## Type Conversions

The plugin provides type-safe accessors for common types:

```kotlin
val host: String   = secrets.asString("server.host")
val port: Int      = secrets.asInt("server.port")
val timeout: Long  = secrets.asLong("server.timeout")
val debug: Boolean = secrets.asBoolean("app.debug")
val rate: Float    = secrets.asFloat("app.rate")
val pi: Double     = secrets.asDouble("math.pi")
```

### Custom Types (Java & Groovy)

Java and Groovy consumers can decode into any Kotlin-compatible class using `asType(key, Class<T>)`:

```java
// Java
MyConfig config = secrets.asType("my.config", MyConfig.class);
```

```groovy
// Groovy
def config = secrets.asType('my.config', MyConfig)
```

Kotlin users can use the reified `get<T>(key)` accessor instead:

```kotlin
val config: MyConfig = secrets.get("my.config")
```

## Default Values

Provide a fallback value to avoid build failures when a secret is not found:

```kotlin
val port: Int = secrets.asInt("server.port", 8080)
val debug: Boolean = secrets.asBoolean("app.debug", false)
```

## Secret Resolution Order

Secrets are resolved using the following priority (highest to lowest):

| Priority | Source                  | Example                                           |
|----------|-------------------------|----------------------------------------------------|
| 1st      | Environment Variables   | `MY_API_KEY=secret123`                             |
| 2nd      | Registered Secret Files | `my.api.key=secret123` in `secrets.properties`     |
| 3rd      | Gradle Properties       | `-Pmy.api.key=secret123` or in `gradle.properties` |

> **Environment variable naming.** Dot-notation keys are converted to `SCREAMING_SNAKE_CASE`:
> - `my.api.key` → `MY_API_KEY`
> - `db.connection.timeout` → `DB_CONNECTION_TIMEOUT`
>
> Matching is case-sensitive and dots become underscores. If both an environment variable and a file entry exist for the same key, the environment variable wins.

## Using Secret Files

Register one or more secret files. Supported formats include `.properties`, `.yaml`, `.toml`, and `.json`. Later files take priority over earlier ones:

```kotlin
secrets {
    file("config/base.properties")
    file("config/local.properties") // values here override base.properties
}
```

## Configuration

The `secrets` extension supports the following configuration options:

| Option                   | Description                                                      | Default   |
|--------------------------|------------------------------------------------------------------|-----------|
| `file(path)`             | Registers a secret file source. Can be called multiple times.    | _none_    |
| `disableEnvironment()`   | Disables resolution from system environment variables.           | _enabled_ |
| `disableGradleProperties()` | Disables resolution from Gradle project properties.          | _enabled_ |

### Full Configuration Example

```kotlin
plugins {
    id("com.lukehackett.gradle.secrets") version "<latest>"
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

---

[← Back to README](../README.md) · [Multi-module Builds →](./multi-module.md)
