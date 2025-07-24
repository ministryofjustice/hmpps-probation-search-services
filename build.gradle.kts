import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.JibTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0" apply false
    kotlin("plugin.jpa") version "2.2.0" apply false
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.google.cloud.tools.jib") version "3.4.5" apply false
    id("idea")
}

allprojects {
    group = "uk.gov.justice.digital"
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
        withType<JibTask> { dependsOn("copyAgent", "copyAgentConfig", "assemble") }
        // Ensure build is reproducible
        withType<Jar> {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            archiveFileName.set("${archiveBaseName.get()}-${archiveClassifier.get()}.${archiveExtension.get()}")
        }
        withType<BootJar> { enabled = false }
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
    }

    idea {
        module {
            if (System.getenv("CI") == null) {
                isDownloadJavadoc = true
                isDownloadSources = true
            }
            // Register dev source set in IntelliJ IDEA
            testSources.from(sourceSets["dev"].allSource.srcDirs)
            testResources.from(sourceSets["dev"].resources.srcDirs)
        }
    }

    pluginManager.withPlugin("com.google.cloud.tools.jib") {
        extensions.configure<JibExtension> {
            container {
                jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
                mainClass = "uk.gov.justice.digital.hmpps.AppKt"
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
}
