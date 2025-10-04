pluginManagement {
    repositories {
        maven {url = uri("https://maven.aliyun.com/repository/google")}
        maven {url = uri("https://maven.aliyun.com/repository/releases")}
        maven {url = uri("https://maven.aliyun.com/repository/central")}
        maven {url = uri("https://maven.aliyun.com/repository/public")}
        maven {url = uri("https://maven.aliyun.com/repository/gradle-plugin")}
        maven {url = uri("https://maven.aliyun.com/repository/gradle-snapshots")}
        maven {url = uri("https://maven.aliyun.com/nexus/content/groups/public/")}
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {url = uri("https://artifact.bytedance.com/repository/Volcengine/")}
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {url = uri("https://maven.aliyun.com/repository/google")}
        maven {url = uri("https://maven.aliyun.com/repository/releases")}
        maven {url = uri("https://maven.aliyun.com/repository/central")}
        maven {url = uri("https://maven.aliyun.com/repository/public")}
        maven {url = uri("https://maven.aliyun.com/repository/gradle-plugin")}
        maven {url = uri("https://maven.aliyun.com/repository/gradle-snapshots")}
        maven {url = uri("https://maven.aliyun.com/nexus/content/groups/public/")}
        google()
        mavenCentral()
        maven {url = uri("https://artifact.bytedance.com/repository/Volcengine/")}
    }
}

rootProject.name = "VolcengineTTS"
include(":app")
 