import com.google.cloud.tools.jib.gradle.BuildDockerTask
import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.google.cloud.tools.jib.gradle.BuildTarTask
import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.JibTask
import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.buildinfo.BuildInfo
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0" apply false
    kotlin("plugin.jpa") version "2.2.0" apply false
    id("org.springframework.boot") version "3.5.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.google.cloud.tools.jib") version "3.4.5" apply false
    id("io.sentry.jvm.gradle") version "5.8.0" apply false
    id("idea")
}

allprojects {
    group = "uk.gov.justice.digital.hmpps.probation.search"
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.jpa")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("com.google.cloud.tools.jib")
        plugin("io.sentry.jvm.gradle")
        plugin("idea")
    }

    dependencies {
        configurations.create("agent")("com.microsoft.azure:applicationinsights-agent:3.7.3")
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict") // to make use of Spring's null-safety annotations
            freeCompilerArgs.add("-Xannotation-default-target=param-property") // see https://youtrack.jetbrains.com/issue/KT-73255
        }
    }

    configurations.create("dev") {
        // Add a "dev" configuration, for dev dependencies. These will not be included in the final image.
        extendsFrom(configurations["implementation"], configurations["runtimeOnly"])
    }

    sourceSets {
        // Add a "dev" source set, for local test data. This will not be included in the final image.
        val main = getByName("main")
        val dev = create("dev") {
            compileClasspath += configurations["dev"] + main.compileClasspath + main.output
            runtimeClasspath += configurations["dev"] + main.runtimeClasspath + main.output
        }
        getByName("test") {
            compileClasspath += configurations["dev"] + dev.output
            runtimeClasspath += configurations["dev"] + dev.output
        }
    }

    tasks {
        // Copy Application Insights agent JAR into build directory
        register<Copy>("copyAgent") {
            from(configurations.getByName("agent"))
            into("${project.layout.buildDirectory.dir("agent").get().asFile}")
            rename("applicationinsights-agent(.+).jar", "agent.jar")
        }
        register<Copy>("copyAgentConfig") {
            from(project.layout.projectDirectory)
            into("${project.layout.buildDirectory.dir("agent").get().asFile}")
            include("applicationinsights.json")
        }
        // Ensure build is reproducible
        withType<Jar> {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            archiveFileName.set("${archiveBaseName.get()}-${archiveClassifier.get()}.${archiveExtension.get()}")
        }
        withType<BootJar> { enabled = false }
        // Generate build info into a different directory so that it isn't included in the final image - to improve caching and reproducibility
        register<BuildInfo>("buildInfo") { destinationDir = layout.buildDirectory.dir("info") }
        // Include the dev source set when running with the dev Spring Boot profile
        withType<BootRun> {
            val profiles = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"))
            if (profiles?.split(",")?.contains("dev") == true) {
                classpath = sourceSets.getByName("dev").runtimeClasspath
            }
        }
        // Use JUnit 5
        withType<Test> {
            systemProperty("spring.profiles.active", "dev")
            useJUnitPlatform()
        }
        // Customise the Jib tasks to enable caching
        fun <T : JibTask> T.configureJibTask(jib: T.() -> JibExtension) {
            dependsOn("copyAgent", "copyAgentConfig", "assemble")
            // Set variable configuration in a doFirst block to avoid invalidating the Gradle cache
            doFirst {
                jib().to {
                    tags = setOf(System.getenv("VERSION") ?: "dev")
                    auth {
                        username = System.getenv("JIB_USERNAME")
                        password = System.getenv("JIB_PASSWORD")
                    }
                }
            }
            if (System.getenv("FORCE_DEPLOY") == "true") {
                jib().to.tags = setOf(System.getenv("VERSION") ?: "dev")
            }
            // Enable caching
            val buildDir = layout.buildDirectory.get().asFile.path
            inputs.files(
                "helm_deploy",
                "$buildDir/agent",
                "$buildDir/classes",
                "$buildDir/generated",
                "$buildDir/libs",
                "$buildDir/resources"
            )
            outputs.files("$buildDir/jib-image.id")
            outputs.cacheIf { true }
            // Generate a marker file to indicate whether a new image was built
            doLast { layout.buildDirectory.file("jib-image.changed").get().asFile.createNewFile() }
        }
        withType<BuildDockerTask> { configureJibTask { jib!! } }
        withType<BuildImageTask> { configureJibTask { jib!! } }
        withType<BuildTarTask> { configureJibTask { jib!! } }
    }

    pluginManager.withPlugin("com.google.cloud.tools.jib") {
        // Configure container image
        extensions.configure<JibExtension> {
            container {
                jvmFlags = mutableListOf("-Duser.timezone=Europe/London -javaagent:/agent/agent.jar")
                mainClass = "uk.gov.justice.digital.hmpps.probation.search.AppKt"
                user = "2000:2000"
            }
            from { image = System.getenv("JIB_FROM_IMAGE") ?: "eclipse-temurin:21-jre-alpine" }
            to { image = "ghcr.io/ministryofjustice/hmpps-probation-search-services/${project.name}" }
            extraDirectories {
                paths {
                    path {
                        setFrom("${project.layout.buildDirectory.dir("agent").get().asFile}")
                        into = "/agent"
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("io.sentry.jvm.gradle") {
        // Upload sources to Sentry
        extensions.configure<SentryPluginExtension> {
            org = "ministryofjustice"
            projectName = "probation-search-api"
            authToken = System.getenv("SENTRY_AUTH_TOKEN")
            includeSourceContext = System.getenv("SENTRY_AUTH_TOKEN") != null
        }
    }

    idea {
        module {
            // Download sources when developing locally
            if (System.getenv("CI") == null) {
                isDownloadJavadoc = true
                isDownloadSources = true
            }
            // Register dev source set in IntelliJ IDEA
            testSources.from(sourceSets["dev"].allSource.srcDirs)
            testResources.from(sourceSets["dev"].resources.srcDirs)
        }
    }
}
