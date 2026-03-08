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
      // Leaf node: only set the value if the key is not already occupied by a subtree.
      // If a Map already exists here (from a longer env-var key), we skip this leaf
      // to avoid overwriting the subtree.
      val existing = map[key]
      if (existing !is MutableMap<*, *>) {
        map[key] = value
      }
    } else {
      val existing = map[key]
      @Suppress("UNCHECKED_CAST")
      val child =
          when (existing) {
            is MutableMap<*, *> -> existing as MutableMap<String, Any>
            else -> {
              // Either no entry yet, or a String leaf from a shorter env-var key.
              // Replace it with a subtree so the longer key can be stored.
              mutableMapOf<String, Any>().also { map[key] = it }
            }
          }
      insert(child, path.drop(1), value)
    }
  }
}
