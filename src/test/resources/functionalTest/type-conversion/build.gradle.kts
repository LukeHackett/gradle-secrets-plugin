plugins {
    id("com.lukehackett.gradle.secrets")
}

tasks.register("printPort") {
    doLast {
        val port = secrets.asInt("server.port")
        println("PORT_TYPE=${port::class.simpleName}")
        println("PORT_VALUE=$port")
    }
}