import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.shadow.jar)
  alias(libs.plugins.publish)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kover)
  alias(libs.plugins.ktfmt)
  `java-gradle-plugin`
  `kotlin-dsl`
  `maven-publish`
}

// Java version in use across the project (must be a major version like "25", not "25.0.1")
val javaVersion = file(".java-version").readText().trim()
val projectName = project.name
val projectDescription = project.description.orEmpty()
val repositoryUrl: String by project

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
      displayName = projectName
      description = projectDescription
      tags.set(listOf("secrets", "configuration", "hoplite", "security"))
    }
  }
  website.set(repositoryUrl)
  vcsUrl.set("$repositoryUrl.git")
}

publishing {
  publications {
    withType<MavenPublication> {
      pom {
        name.set(projectName)
        description.set(projectDescription)
        url.set(repositoryUrl)
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

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(javaVersion))
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("")

  // Relocate Hoplite to prevent version conflicts in consumer projects
  relocate("com.sksamuel.hoplite", "$group.shadow.hoplite")
  mergeServiceFiles()
}

tasks.named<Jar>("jar") { enabled = false }

tasks.named<Test>("test") {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging {
    events("passed", "skipped", "failed")
    showStandardStreams = true
    showExceptions = true
    showStackTraces = true
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}
