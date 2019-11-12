package com.company.kotlinconsolidatedplugin

import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.JibPlugin
import com.gorylenko.GitPropertiesPlugin
import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.GitAdapter
import net.researchgate.release.ReleaseExtension
import net.researchgate.release.ReleasePlugin
import org.asciidoctor.gradle.AsciidoctorPlugin
import org.asciidoctor.gradle.AsciidoctorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.sonarqube.gradle.SonarQubeExtension
import org.sonarqube.gradle.SonarQubePlugin
import java.io.File
import java.net.URI

open class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(GitPropertiesPlugin::class.java)

        applyAsciidoctor(project)
        applySonar(project)
        applyPublish(project)
        applyJib(project)
        applyRelease(project)
    }

    private fun applyAsciidoctor(project: Project) {
        val snippetsDir = File("${project.buildDir}/generated-snippets")
        val javadocJsonDir = File("${project.buildDir}/generated-javadoc-json")

        project.pluginManager.apply(DokkaPlugin::class.java)
        project.pluginManager.apply(AsciidoctorPlugin::class.java)

        project.tasks.withType(DokkaTask::class.java).configureEach {
            it.outputFormat = "auto-restdocs-json"
            it.outputDirectory = javadocJsonDir.toString()
            it.includeNonPublic = true
            it.dokkaFatJar = "capital.scalable:spring-auto-restdocs-dokka-json:2.0.6"
        }

        project.tasks.withType(Test::class.java).configureEach {
            it.systemProperty("org.springframework.restdocs.outputDir", snippetsDir)
            it.systemProperty("org.springframework.restdocs.javadocJsonDir", javadocJsonDir)
            it.dependsOn(project.tasks.withType(DokkaTask::class.java))
            it.finalizedBy(project.tasks.withType(JacocoReport::class.java))
        }

        project.tasks.withType(AsciidoctorTask::class.java).configureEach {
            it.sourceDir = File("${project.rootDir}/src/main/asciidoc")
            it.outputDir = File("${project.buildDir}/generated-docs")
            it.options["backend"] = "html"
            it.options["doctype"] = "book"
            it.attributes["source-highlighter"] = "highlightjs"
            it.attributes["snippets"] = snippetsDir
            it.doLast { File("${project.buildDir}/generated-docs/html5/index.html").copyTo(File("${project.buildDir}/resources/main/static/docs/index.html")) }
            it.dependsOn(project.tasks.withType(Test::class.java))
        }
        project.configurations.create("asciidoctorj") // By default, the AsciidoctorPlugin uses asciidoctorj so running asciidoctor will fail if this is not created
    }

    private fun applySonar(project: Project) {
        project.pluginManager.apply(SonarQubePlugin::class.java)

        project.tasks.withType(JacocoReport::class.java).configureEach {
            it.reports.html.isEnabled = false
            it.reports.xml.isEnabled = true
        }

        project.extensions.findByType(SonarQubeExtension::class.java)?.properties {
            it.property("sonar.sources", File("src/main"))
        }
    }

    private fun applyPublish(project: Project) {
        project.pluginManager.apply(PublishingPlugin::class.java)

        val publishingExtension = project.extensions.findByType(PublishingExtension::class.java)
        val mavenPublication = publishingExtension?.publications?.create(project.name, MavenPublication::class.java)

        publishingExtension?.repositories?.maven {
            val baseUrl = "https://${project.properties["nexus.host"].toString()}:${project.properties["nexus.port.jar"].toString()}/repository"
            it.url = URI(if (project.version.toString().endsWith("SNAPSHOT")) "$baseUrl/maven-snapshots" else "$baseUrl/maven-releases")
            it.credentials { cred ->
                cred.username = project.properties["nexus.user"].toString()
                cred.password = project.properties["nexus.password"].toString()
            }
        }

        mavenPublication?.from(project.components.findByName("java"))
        mavenPublication?.pom?.scm {
            it.connection.set("scm:git:git@github.com:diendanyoi54/${project.name}.git")
            it.developerConnection.set("scm:git:git@github.com:diendanyoi54/${project.name}.git")
            it.url.set("https://github.com/diendanyoi54/${project.name}/")
        }
    }

    private fun applyJib(project: Project) {
        project.pluginManager.apply(JibPlugin::class.java)

        val jibExtension = project.extensions.findByType(JibExtension::class.java)
        jibExtension?.from { it.image = "openjdk:8-jdk-alpine" }
        jibExtension?.to {
            it.image = "${project.properties["nexus.host"].toString()}:${project.properties["nexus.port.image"].toString()}/${project.name}:${project.version}"
            it.auth { auth ->
                auth.username = project.properties["nexus.user"].toString()
                auth.password = project.properties["nexus.password"].toString()
            }
        }
        jibExtension?.container {
            it.workingDirectory = "/"
            it.ports = listOf("8080")
            it.environment = mapOf(
                    "SPRING_OUTPUT_ANSI_ENABLED" to "ALWAYS",
                    "SPRING_CLOUD_BOOTSTRAP_LOCATION" to if (project.name == "cloud-server") project.properties["bootstrap.location.cloud-server"].toString() else project.properties["bootstrap.location.cloud-client"].toString()
            )
            it.useCurrentTimestamp = true
        }
        jibExtension?.setAllowInsecureRegistries(true)
    }

    private fun applyRelease(project: Project) {
        project.pluginManager.apply(ReleasePlugin::class.java)

        val releaseExtension = project.extensions.findByType(ReleaseExtension::class.java)

        releaseExtension?.scmAdapters = mutableListOf<Class<out BaseScmAdapter>> ( GitAdapter::class.java )
        releaseExtension?.git {
            requireBranch = "develop"
            pushToRemote = project.properties["release.git.remote"].toString()
        }
        releaseExtension?.pushReleaseVersionBranch = "master"
        releaseExtension?.tagTemplate = "${project.name}.${project.version}"
    }

    private fun ReleaseExtension.git(configureFn : GitAdapter.GitConfig.() -> Unit) {
        (propertyMissing("git") as GitAdapter.GitConfig).configureFn()
    }
}