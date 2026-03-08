package com.lukehackett.gradle.secrets.source

import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.PropertySourceContext

class EnvironmentPropertySource(
    private val env: Map<String, String> = System.getenv(),
) : PropertySource {
  override fun source(): String = "Single Underscore Environment Source"

  override fun node(context: PropertySourceContext): ConfigResult<Node> {
    // Use the injected 'env' instead of calling System.getenv() directly
    val map =
        env.entries.fold(mutableMapOf<String, Any>()) { acc, (key, value) ->
          val path = key.lowercase().split("_")
          insert(acc, path, value)
          acc
        }
    return PropertySource.map(map).node(context)
  }

  private fun insert(
      map: MutableMap<String, Any>,
      path: List<String>,
      value: String,
  ) {
    if (path.isEmpty()) return

    val key = path.first()
    if (path.size == 1) {
      map[key] = value
    } else {
      @Suppress("UNCHECKED_CAST")
      val child = map.computeIfAbsent(key) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
      insert(child, path.drop(1), value)
    }
  }
}
