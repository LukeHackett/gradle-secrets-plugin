package com.lukehackett.gradle.secrets.source

import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.Undefined
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
  fun `should not throw when short key conflicts with longer key sharing same prefix`() {
    // Simulates CI environments where e.g. GITHUB_ACTION and GITHUB_ACTION_PATH both exist.
    // The shorter key ("github.action" = "run1") would be stored as a String first, then
    // the longer key ("github.action.path" = "/home/...") needs to replace it with a Map.
    val fakeEnv =
        mapOf(
            "GITHUB_ACTION" to "run1",
            "GITHUB_ACTION_PATH" to "/home/runner/actions",
            "GITHUB_ACTION_REPOSITORY" to "owner/repo",
        )
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe() as MapNode

    // Then — should not throw ClassCastException and the subtree should be navigable
    val github = node.atKey("github") as MapNode
    val action = github.atKey("action") as MapNode
    assertEquals("/home/runner/actions", (action.atKey("path") as StringNode).value)
    assertEquals("owner/repo", (action.atKey("repository") as StringNode).value)
  }

  @Test
  fun `should preserve subtree when leaf key arrives after longer keys`() {
    // Given — longer keys processed first build a subtree, then the short key arrives as a leaf.
    // The leaf should be skipped to preserve the subtree.
    val fakeEnv =
        mapOf(
            "APP_PORT_HTTP" to "8080",
            "APP_PORT_HTTPS" to "8443",
            "APP_PORT" to "3000",
        )
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe() as MapNode

    // Then — the subtree under "app.port" is preserved; the leaf "APP_PORT" does not overwrite it
    val app = node.atKey("app") as MapNode
    val port = app.atKey("port") as MapNode
    assertEquals("8080", (port.atKey("http") as StringNode).value)
    assertEquals("8443", (port.atKey("https") as StringNode).value)
  }

  @Test
  fun `should handle deeply nested key conflicts`() {
    // Given — multiple levels of prefix conflicts
    val fakeEnv =
        mapOf(
            "A_B" to "shallow",
            "A_B_C" to "mid",
            "A_B_C_D" to "deep",
        )
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe() as MapNode

    // Then — deepest path should be navigable without ClassCastException
    val a = node.atKey("a") as MapNode
    val b = a.atKey("b") as MapNode
    val c = b.atKey("c") as MapNode
    assertEquals("deep", (c.atKey("d") as StringNode).value)
  }

  @Test
  fun `should handle single segment key alongside multi segment key with same prefix`() {
    // Given
    val fakeEnv = mapOf("CI" to "true", "CI_NAME" to "github-actions")
    val source = EnvironmentPropertySource(fakeEnv)

    // When — "CI" becomes a leaf at "ci", but "CI_NAME" needs "ci" to be a Map
    val node = source.node(context).getUnsafe() as MapNode

    // Then — the subtree should exist and be navigable
    val ci = node.atKey("ci") as MapNode
    assertEquals("github-actions", (ci.atKey("name") as StringNode).value)
  }

  @Test
  fun `should handle empty environment`() {
    // Given
    val fakeEnv = emptyMap<String, String>()
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe()

    // Then — an empty map source yields an empty MapNode or Undefined
    assertTrue(node is Undefined || (node is MapNode && node.size == 0))
  }

  @Test
  fun `should handle single entry environment`() {
    // Given
    val fakeEnv = mapOf("SOLO_KEY" to "only-value")
    val source = EnvironmentPropertySource(fakeEnv)

    // When
    val node = source.node(context).getUnsafe() as MapNode

    // Then
    val solo = node.atKey("solo") as MapNode
    assertEquals("only-value", (solo.atKey("key") as StringNode).value)
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
