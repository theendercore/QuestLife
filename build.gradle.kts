import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.3.8"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("org.teamvoided.iridium") version "3.1.0"
}

group = project.properties["maven_group"]!!
version = project.properties["mod_version"]!!
base.archivesName.set(project.properties["archives_base_name"] as String)
description = "Quest Life is a mod thats inspired by Grians secret life series. Made for the Cornerstone version of it."
val modid: String by project
val server_translations: String by project
val sqlite_jdbc: String by project

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.nucleoid.xyz") }
        filter { includeGroup("xyz.nucleoid") }
    }
}

modSettings {
    modId(modid)
    modName("Quest Life")

    entrypoint("main", "com.theendercore.quest_life.QuestLife::commonInit")
}
dependencies {
    modImplementation(include("maven.modrinth", "sqlite-jdbc", sqlite_jdbc))
    modImplementation(include("xyz.nucleoid", "server-translations-api", server_translations))
}

tasks {
    val targetJavaVersion = 17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = targetJavaVersion.toString()
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(JavaVersion.toVersion(targetJavaVersion).toString()))
        withSourcesJar()
    }
}