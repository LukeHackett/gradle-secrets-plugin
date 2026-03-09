package com.lukehackett.gradle.secrets.extension

import com.lukehackett.gradle.secrets.source.EnvironmentPropertySource
import com.lukehackett.gradle.secrets.source.GradlePropertySource
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.fp.getOrElse
import java.io.File
import javax.inject.Inject
import kotlin.reflect.KClass
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout

/**
 * Extension for retrieving sensitive configuration values across different environments.
 *
 * This extension provides a consistent way to access secrets while maintaining a strict precedence order. It is
 * designed to be friendly to both Kotlin and Java/Groovy users.
 *
 * This extension is **configuration-cache compatible**: it does not hold a reference to [org.gradle.api.Project].
 * Instead, it captures only serializable data (file paths, property snapshots, and the injectable [ProjectLayout]).
 *
 * ### Example Usage in build.gradle.kts:
 * ```kotlin
 * secrets {
 *   // Register multiple secret files (later files override earlier ones)
 *   file("config/base.properties")
 *   file("config/local.properties")
 *
 *   // Optional: Disable retrieving values from Environment variables
 *   disableEnvironment()
 *
 *   // Optional: Disable retrieve values from the gradle properties (local and global)
 *   disableGradleProperties()
 * }
 *
 * tasks.register("example") {
 *   doLast {
 *     val apiKey: String = secrets.get("api.key")
 *     val timeout: Int = secrets.asInt("api.timeout", 5000)
 *     println("Fetching data with key: $apiKey")
 *   }
 * }
 * ```
 */
abstract class GradleSecretsExtension
@Inject
constructor(
    private val layout: ProjectLayout,
) {
  private val customFiles = mutableListOf<File>()
  private var useEnv = true
  private var useGradle = true

  /**
   * A snapshot of the Gradle project properties captured at configuration time. This avoids retaining a
   * [org.gradle.api.Project] reference which is not serializable for the configuration cache.
   */
  internal var gradleProperties: Map<String, String> = emptyMap()

  /**
   * Adds a file to the list of secret sources. Later files in the list will override values from earlier files.
   *
   * @param path A path relative to the project directory (e.g., "secrets.properties" or "config/auth.properties").
   */
  fun file(path: Any) {
    customFiles.add(layout.projectDirectory.file(path.toString()).asFile)
  }

  /** Disables the loading of values from System Environment Variables. */
  fun disableEnvironment() {
    useEnv = false
  }

  /** Disables the loading of values from Gradle Project properties (gradle.properties). */
  fun disableGradleProperties() {
    useGradle = false
  }

  /**
   * Retrieves a secret and automatically decodes it to type [T]. Primarily for Kotlin users leveraging reified types.
   *
   * @param T The target type for decoding.
   * @param key The dot-notation path to the secret (e.g., "db.password").
   * @param default An optional fallback value if the key is missing.
   * @return The decoded secret value of type [T].
   * @throws GradleException if the secret is missing and no default is provided.
   */
  inline fun <reified T : Any> get(
      key: String,
      default: T? = null,
  ): T = getInternal(key, T::class, default)

  /**
   * Retrieves a secret as a [String].
   *
   * @param key The dot-notation path to the secret.
   * @param default An optional fallback value.
   * @return The resolved String value.
   */
  fun asString(
      key: String,
      default: String? = null,
  ): String = getInternal(key, String::class, default)

  /**
   * Retrieves a secret as an [Int].
   *
   * @param key The dot-notation path to the secret.
   * @param default An optional fallback value.
   * @return The resolved Integer value.
   */
  fun asInt(
      key: String,
      default: Int? = null,
  ): Int = getInternal(key, Int::class, default)

  /**
   * Retrieves a secret as a [Long].
   *
   * @param key The dot-notation path to the secret.
   * @param default An optional fallback value.
   * @return The resolved Long value.
   */
  fun asLong(
      key: String,
      default: Long? = null,
  ): Long = getInternal(key, Long::class, default)

  /**
   * Retrieves a secret as a [Boolean].
   *
   * @param key The dot-notation path to the secret.
   * @param default An optional fallback value.
   * @return The resolved Boolean value.
   */
  fun asBoolean(
      key: String,
      default: Boolean? = null,
  ): Boolean = getInternal(key, Boolean::class, default)

  /**
   * Retrieves a secret as a [Float].
   *
   * @param key The dot-notation path to the secret.
   * @param default An optional fallback value.
   * @return The resolved Float value.
   */
  fun asFloat(
      key: String,
      default: Float? = null,
  ): Float = getInternal(key, Float::class, default)

  /**
   * Retrieves a secret as a [Double].
   *
   * @param key The dot-notation path to the secret.
   * @param default An optional fallback value.
   * @return The resolved Double value.
   */
  fun asDouble(
      key: String,
      default: Double? = null,
  ): Double = getInternal(key, Double::class, default)

  /**
   * Generic fallback for Java users to decode into custom classes or records.
   *
   * @param T The target type.
   * @param key The dot-notation path to the secret.
   * @param clazz The Java class representing the target type.
   * @return The decoded instance of type [T].
   */
  fun <T : Any> asType(
      key: String,
      clazz: Class<T>,
  ): T = getInternal(key, clazz.kotlin, null)

  /**
   * Internal bridging method to handle the actual Hoplite binding logic.
   *
   * @param T The target type.
   * @param key The dot-notation path to the secret.
   * @param klass The Kotlin class representing the target type.
   * @param default Fallback value to return if lookup fails.
   * @return The decoded value.
   * @throws GradleException if the secret is missing and no default is provided.
   */
  @PublishedApi
  internal fun <T : Any> getInternal(
      key: String,
      klass: KClass<T>,
      default: T?,
  ): T {
    // Hoplite discovers many decoders via ServiceLoader which uses the thread context ClassLoader.
    // During Gradle configuration (especially Kotlin DSL), the context ClassLoader can differ from the
    // plugin's ClassLoader, causing Hoplite to "see" no decoders.
    val thread = Thread.currentThread()
    val originalContextClassLoader = thread.contextClassLoader
    thread.contextClassLoader = javaClass.classLoader
    try {
      val loader = createLoader()

      // Hoplite's binder has multiple overloads for bind(...). Under Gradle's embedded Kotlin (Kotlin DSL),
      // passing a KClass with an out-projection can cause the wrong overload to be selected.
      // We explicitly cast back to KClass<T> to force the generic bind overload.
      @Suppress("UNCHECKED_CAST") val resolvedClass = resolveBoxedClass(klass) as KClass<T>

      return loader.configBinder().bind(resolvedClass, key).getOrElse { failure ->
        default
            ?: throw GradleException(
                "Secret '$key' not found or could not be decoded as ${klass.simpleName}.\n" +
                    "Reason: ${failure.description()}",
            )
      } as T
    } finally {
      thread.contextClassLoader = originalContextClassLoader
    }
  }

  /**
   * Maps Kotlin primitive [KClass] types to their JVM boxed equivalents so that Hoplite's binder resolves them
   * consistently across all JDK versions.
   */
  private fun <T : Any> resolveBoxedClass(klass: KClass<T>): KClass<out Any> =
      when (klass) {
        Int::class -> Integer::class
        Long::class -> java.lang.Long::class
        Float::class -> java.lang.Float::class
        Double::class -> java.lang.Double::class
        Boolean::class -> java.lang.Boolean::class
        Short::class -> java.lang.Short::class
        Byte::class -> java.lang.Byte::class
        Char::class -> Character::class
        else -> klass
      }

  /**
   * Create the loader dynamically. Note: For extreme performance in large builds, you could cache this, but for a
   * Secrets plugin, accuracy across task execution is usually better.
   */
  @OptIn(ExperimentalHoplite::class)
  @PublishedApi
  internal fun createLoader(): ConfigLoader =
      ConfigLoaderBuilder.Companion.empty()
          .apply {
            // 1. Intelligence
            addDefaultDecoders()
            addDefaultParsers()
            addDefaultPreprocessors()
            addDefaultParamMappers() // Essential for mapping my.api.key

            // 2. Environment (High Priority)
            if (useEnv) {
              addPropertySource(EnvironmentPropertySource())
            }

            // 3. Custom Files
            customFiles.reversed().forEach { file -> addFileSource(file, optional = true) }

            // 4. Gradle Properties
            if (useGradle) {
              addPropertySource(GradlePropertySource(gradleProperties))
            }

            // 5. Explicitly handle the sealed type warning
            withExplicitSealedTypes()
          }
          .build()
}
