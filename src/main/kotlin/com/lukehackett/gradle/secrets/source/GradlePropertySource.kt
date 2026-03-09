package com.lukehackett.gradle.secrets.source

import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.Pos
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.Undefined
import com.sksamuel.hoplite.decoder.DotPath
import com.sksamuel.hoplite.fp.Validated

/**
 * A Hoplite [PropertySource] that provides access to Gradle project properties. Handles dot-notation by expanding flat
 * keys into a nested tree structure.
 *
 * This class accepts a pre-built [Map] of properties instead of a [org.gradle.api.Project] reference, making it safe
 * for Gradle's configuration cache (no non-serializable objects are retained).
 */
class GradlePropertySource(
    private val properties: Map<String, String>,
) : PropertySource {
  override fun source(): String = "Gradle Project Properties"

  override fun node(context: PropertySourceContext): ConfigResult<Node> {
    if (properties.isEmpty()) return Validated.Valid(Undefined)

    // We convert the flat map "a.b.c" -> "value" into a nested Map tree
    val rootMap = mutableMapOf<String, Any?>()
    properties.forEach { (key, value) ->
      val parts = key.split(".")
      var current = rootMap
      for (i in 0 until parts.size - 1) {
        @Suppress("UNCHECKED_CAST")
        current = current.getOrPut(parts[i]) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
      }
      current[parts.last()] = value
    }

    // Recursively convert the nested Map tree into Hoplite Nodes
    fun transform(
        value: Any?,
        path: DotPath,
    ): Node =
        when (value) {
          is Map<*, *> -> {
            val children = value.map { (k, v) -> k.toString() to transform(v, path.with(k.toString())) }.toMap()
            MapNode(children, Pos.NoPos, path)
          }
          else -> StringNode(value.toString(), Pos.NoPos, path)
        }

    return Validated.Valid(transform(rootMap, DotPath.Companion.root))
  }
}
