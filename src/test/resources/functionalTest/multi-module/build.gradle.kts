allprojects {
    tasks.register("printSecret") {
        doLast {
            val key = project.findProperty("targetKey") as? String ?: "my.api.key"
            val value = secrets.asString(key)
            println("PROJECT=${project.name} SECRET_VALUE=$value")
        }
    }
}
