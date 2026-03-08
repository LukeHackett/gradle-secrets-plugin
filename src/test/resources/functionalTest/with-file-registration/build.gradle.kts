plugins {
    id("com.lukehackett.gradle.secrets")
}

secrets {
    file("secrets.properties")
}

tasks.register("printSecret") {
    doLast {
        val value = secrets.asString("my.api.key")
        println("SECRET_VALUE=$value")
    }
}