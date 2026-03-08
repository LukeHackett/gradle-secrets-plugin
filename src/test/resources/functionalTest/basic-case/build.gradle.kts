plugins {
    id("com.lukehackett.gradle.secrets")
}

tasks.register("printSecret") {
    doLast {
        val key = project.findProperty("targetKey") as? String ?: "my.api.key"
        val value = secrets.asString(key)
        println("SECRET_VALUE=$value")
    }
}