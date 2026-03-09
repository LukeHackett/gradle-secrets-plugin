package com.lukehackett.gradle.secrets.source

import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.Undefined
import com.sksamuel.hoplite.fp.Validated
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class GradlePropertySourceTest {
  private val context = mockk<PropertySourceContext>()

  @Test
  fun `should return Undefined when project has no properties`() {
    // Given
    val propertySource = GradlePropertySource(emptyMap())

    // When
    val result = propertySource.node(context)

    // Then
    assertTrue(result is Validated.Valid)
    val node = (result as Validated.Valid).value
    assertTrue(node is Undefined, "Expected node to be Undefined but was ${node::class.simpleName}")
  }

  @Test
  fun `should explode dot-notation keys into nested nodes`() {
    // Given
    val props = mapOf("server.port" to "8080")
    val propertySource = GradlePropertySource(props)

    // When
    val result = propertySource.node(context)

    // Then
    val root = (result as Validated.Valid).value as MapNode
    val serverNode = root.map["server"] as MapNode
    val portNode = serverNode.map["port"] as StringNode
    assertEquals("8080", portNode.value)
  }

  @Test
  fun `should handle multiple nested properties under same prefix`() {
    // Given
    val props = mapOf("db.host" to "localhost", "db.port" to "5432")
    val propertySource = GradlePropertySource(props)

    // When
    val result = propertySource.node(context)

    // Then
    val root = (result as Validated.Valid).value as MapNode
    val dbNode = root.map["db"] as MapNode
    assertEquals("localhost", (dbNode.map["host"] as StringNode).value)
    assertEquals("5432", (dbNode.map["port"] as StringNode).value)
  }

  @Test
  fun `should include all provided string properties`() {
    // Given — filtering of non-string values now happens at snapshot time in the plugin,
    // so GradlePropertySource always receives a Map<String, String>.
    val props = mapOf("api.key" to "secret-value")
    val propertySource = GradlePropertySource(props)

    // When
    val result = propertySource.node(context)

    // Then
    val root = (result as Validated.Valid).value as MapNode
    val apiNode = root.map["api"] as MapNode
    assertEquals(1, apiNode.map.size)
    assertTrue(apiNode.map.containsKey("key"))
  }

  @Test
  fun `should report correct source name`() {
    // Given
    val propertySource = GradlePropertySource(emptyMap())

    // When
    val sourceName = propertySource.source()

    // Then
    assertEquals("Gradle Project Properties", sourceName)
  }
}
