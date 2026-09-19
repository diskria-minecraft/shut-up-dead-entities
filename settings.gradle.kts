pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        gradlePluginPortal()
    }
}

rootProject.name = "shut_up_dead_entities"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
    }
}
