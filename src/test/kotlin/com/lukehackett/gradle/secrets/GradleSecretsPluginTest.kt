package com.lukehackett.gradle.secrets

import com.lukehackett.gradle.secrets.extension.GradleSecretsExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GradleSecretsPluginTest {
  @Test
  fun `plugin registers secrets extension when applied`() {
    // Given
    val project: Project = ProjectBuilder.builder().build()

    // When
    project.plugins.apply(GradleSecretsPlugin::class.java)

    // Then
    val extension = project.extensions.findByName("secrets")
    assertNotNull(extension, "The 'secrets' extension should be registered")
    assertTrue(extension is GradleSecretsExtension, "Extension should be an instance of GradleSecretsExtension")
  }

  @Test
  fun `plugin can be applied using its ID`() {
    // Given
    val project: Project = ProjectBuilder.builder().build()

    // When
    project.plugins.apply("com.lukehackett.gradle.secrets")

    // Then
    assertNotNull(project.extensions.findByName("secrets"))
  }
}
