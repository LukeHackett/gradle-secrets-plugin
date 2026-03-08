package com.lukehackett.gradle.secrets.source

import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.StringNode
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentPropertySourceTest {
  private val context = mockk<PropertySourceContext>()

  @Test
  fun `should transform screaming snake case into nested nodes`() {
    // Given
    val fakeEnv = mapOf("MY_API_KEY" to "secret-123", "DATABASE_PORT" to "5432")
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe() as MapNode

    // Then
    // Verify 'my.api.key' nesting
    val my = node.atKey("my") as MapNode
    val api = my.atKey("api") as MapNode
    val key = api.atKey("key") as StringNode
    assertEquals("secret-123", key.value)

    // Verify 'database.port' nesting
    val db = node.atKey("database") as MapNode
    val port = db.atKey("port") as StringNode
    assertEquals("5432", port.value)
  }

  @Test
  fun `should handle overlapping paths correctly`() {
    // Given
    val fakeEnv = mapOf("APP_NAME" to "myapp", "APP_VERSION" to "1.0.0")
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe() as MapNode

    // Then
    val app = node.atKey("app") as MapNode
    assertEquals("myapp", (app.atKey("name") as StringNode).value)
    assertEquals("1.0.0", (app.atKey("version") as StringNode).value)
  }

  @Test
  fun `should report correct source name`() {
    // Given
    val fakeEnv = mapOf<String, String>()
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val sourceName = source.source()

    // Then
    assertEquals("Single Underscore Environment Source", sourceName)
  }
}
