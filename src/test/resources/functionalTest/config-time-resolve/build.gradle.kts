plugins {
    id("com.lukehackett.gradle.secrets")
}
tasks.register("printSecret") {
    val value = secrets.asString("my.api.key")
    doLast {
        println("SECRET_VALUE=$value")
    }
}
