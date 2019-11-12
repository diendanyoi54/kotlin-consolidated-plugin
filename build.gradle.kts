import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.GitAdapter
import net.researchgate.release.ReleaseExtension

plugins { // This is a runtime script preventing plugins declared here to be accessible in CustomPlugin.kt but is used to actually publish/release this plugin itself
    id("net.researchgate.release") version "2.8.1"
    kotlin("jvm") version "1.3.0"
    `maven-publish`
}

repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") } // This is required to be able to import plugins below in the dependencies
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    // These must be declared here (at compile-time) in order to access in CustomPlugin.kt
    compile(group = "gradle.plugin.com.gorylenko.gradle-git-properties", name = "gradle-git-properties", version = "2.2.0")
    compile(group = "gradle.plugin.com.google.cloud.tools", name = "jib-gradle-plugin", version = "1.7.0")
    compile(group = "net.researchgate", name = "gradle-release", version = "2.8.1")
    compile(group = "org.asciidoctor", name = "asciidoctor-gradle-plugin", version = "1.5.9.2")
    compile(group = "org.jetbrains.dokka", name = "dokka-gradle-plugin", version = "0.9.18")
    compile(group = "org.sonarsource.scanner.gradle", name = "sonarqube-gradle-plugin", version = "2.8")

    implementation(gradleApi()) // This exposes the gradle API to CustomPlugin.kt
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
            pom {
                scm {
                    connection.set("scm:git:git@github.com:diendanyoi54/${project.name}.git")
                    developerConnection.set("scm:git:git@github.com:diendanyoi54/${project.name}.git")
                    url.set("https://github.com/diendanyoi54/${project.name}/")
                }
            }
        }
    }
    repositories {
        maven {
            val baseUrl = "https://${project.properties["nexus.host"].toString()}:${project.properties["nexus.port.jar"].toString()}/repository"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) "$baseUrl/maven-snapshots" else "$baseUrl/maven-releases")
            credentials {
                username = project.properties["nexus.user"].toString()
                password = project.properties["nexus.password"].toString()
            }
        }
    }
}

fun ReleaseExtension.git(configureFn : GitAdapter.GitConfig.() -> Unit) {
    (propertyMissing("git") as GitAdapter.GitConfig).configureFn()
}

release {
    scmAdapters = mutableListOf<Class<out BaseScmAdapter>> ( GitAdapter::class.java )

    git {
        requireBranch = "develop"
        pushToRemote = project.properties["release.git.remote"].toString()
        pushReleaseVersionBranch = "master"
        tagTemplate = "${project.name}.${project.version}"
    }
}