rootProject.name = "probation-search-services"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("azure-app-insights", "com.microsoft.azure:applicationinsights-web:3.7.3")
            library("opensearch-starter", "org.opensearch.client:spring-data-opensearch-starter:2.0.0")
            library("opensearch-client", "org.opensearch.client:opensearch-java:3.2.0")
            library("testcontainers-opensearch", "org.opensearch:opensearch-testcontainers:3.0.2")
            library("sentry", "io.sentry:sentry-spring-boot-starter-jakarta:8.17.0")
            library("springdoc", "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
            bundle("telemetry", listOf("azure-app-insights", "sentry"))
        }
    }
}

plugins {
    id("com.gradle.develocity") version "4.1"
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI") != null }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

// include all project directories
File(rootProject.projectDir, "projects")
    .listFiles { project -> project.isDirectory && File(project, "build.gradle.kts").run { exists() && isFile } }
    .forEach { include(it.name) }

// load children from the "projects" directory (and drop the prefix)
fun ProjectDescriptor.allChildren(): Set<ProjectDescriptor> = children + children.flatMap { it.allChildren() }
rootProject.allChildren()
    .filter { !it.path.startsWith(":libs") }
    .forEach { it.projectDir = File(rootDir, "projects/${it.projectDir.relativeTo(rootDir)}") }
