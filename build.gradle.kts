import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "space.chunks.custom-names"
    version = "1.0.7"

    repositories {
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public")
    }
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("com.gradleup.shadow")
    }

    dependencies {
        implementation(rootProject.libs.kotlin.jvm)
        compileOnly(rootProject.libs.paper.api)
    }

    kotlin {
        jvmToolchain(25)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.shadowJar {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }

    tasks.processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }
}