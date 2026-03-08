import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow.jar)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kover)
  alias(libs.plugins.ktfmt)
  `java-gradle-plugin`
  `kotlin-dsl`
  `maven-publish`
}

group = "com.lukehackett.gradle.secrets"

version = "1.0.0"

// Java version in use across the project (must be a major version like "25", not "25.0.1")
val javaVersion = file(".java-version").readText().trim()

val pluginName = "Gradle Secrets Plugin"
val pluginDescription =
    "A type-safe gradle plugin for retrieving sensitive configuration from environment variables, files, and Gradle properties"
val pluginRepositoryUrl = "https://github.com/LukeHackett/gradle-secrets-plugin"

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt())) }
  withSourcesJar()
  withJavadocJar()
}

repositories { mavenCentral() }

dependencies {
  // Project Dependency
  implementation(libs.bundles.hoplite)

  // Test Dependencies
  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
  testImplementation(libs.mockk)
  testImplementation(libs.bundles.junit)
  testRuntimeOnly(libs.junit.platform.launcher)
}

detekt { config.setFrom(file("config/detekt/detekt.yml")) }

ktfmt { maxWidth.set(120) }

kover {
  reports {
    total {
      log { onCheck = true }
      html { onCheck = true }
      xml { onCheck = true }
    }
    verify { rule { bound { minValue = 85 } } }
  }
}

gradlePlugin {
  plugins {
    create("secrets") {
      id = "$group"
      implementationClass = "$group.GradleSecretsPlugin"
      displayName = pluginName
      description = pluginDescription
      tags.set(listOf("secrets", "configuration", "hoplite", "security"))
    }
  }
  website.set(pluginRepositoryUrl)
  vcsUrl.set("$pluginRepositoryUrl.git")
}

publishing {
  publications {
    create<MavenPublication>("pluginMaven") {
      // Tells Gradle to publish the shadow JAR instead of the standard thin JAR
      project.shadow.component(this)

      pom {
        name.set(pluginName)
        description.set(pluginDescription)
        url.set(pluginRepositoryUrl)
        licenses {
          license {
            name.set("MIT License")
            url.set("${pom.url}/blob/main/LICENSE")
          }
        }
        developers {
          developer {
            id.set("LukeHackett")
            name.set("Luke Hackett")
          }
        }
      }
    }
  }
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("")

  // Relocate Hoplite to prevent version conflicts in consumer projects
  relocate("com.sksamuel.hoplite", "$group.shadow.hoplite")
  mergeServiceFiles()
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(javaVersion))
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
    showStandardStreams = true
    showExceptions = true
    showStackTraces = true
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}
