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
val modid = project.properties["modid"]!! as String

repositories {
    mavenCentral()
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
    }
}

modSettings {
    modId(modid)
    modName("Quest Life")

    entrypoint("main", "com.theendercore.quest_life.QuestLife::commonInit")
}
dependencies {
    modImplementation ("maven.modrinth:sqlite-jdbc:3.41.2.1+20230506")
    include("maven.modrinth:sqlite-jdbc:3.41.2.1+20230506")
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