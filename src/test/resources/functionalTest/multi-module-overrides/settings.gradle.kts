plugins {
    id("com.lukehackett.gradle.secrets.settings")
}

secrets {
    file("config/shared.properties")
}

rootProject.name = "multi-module-overrides-test"
include("api", "web")
