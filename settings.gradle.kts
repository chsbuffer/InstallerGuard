pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        flatDir {
            dirs("libs")
        }
        google()
        mavenCentral()
        maven(url = "https://api.xposed.info")
    }
}

rootProject.name = "Shizuku Installer Guard"

include(":app")
