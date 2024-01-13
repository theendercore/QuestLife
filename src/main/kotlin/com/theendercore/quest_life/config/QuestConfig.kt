package com.theendercore.quest_life.config

import com.google.gson.JsonParser
import com.theendercore.quest_life.QuestLife.MODID
import com.theendercore.quest_life.QuestLife.log
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object QuestConfig {
    private const val CURRENT_VERSION = 1.0
    private val SUPPORTED_VERSIONS = mapOf(Pair(CURRENT_VERSION, ConfigData.serializer()))
    private val configFile: File = FabricLoader.getInstance().configDir.resolve("$MODID.json").toFile()

    var config: ConfigData = ConfigData()

    private val json = Json { prettyPrint = true }
    fun load(): Int {
        if (!configFile.exists()) {
            log.info("No config file found! Creating a new one.")
            save(ConfigData())
            return 0
        }
        try {
            val stringData = FileReader(configFile).use { it.readText() }
            if (stringData.isNotEmpty()) {
                val jsonObject = JsonParser.parseString(stringData).asJsonObject
                if (jsonObject.has("version")) {
                    val configVersion = jsonObject.getAsJsonPrimitive("version").asDouble
                    if (configVersion == CURRENT_VERSION) {
                        config = json.decodeFromString(stringData)
                        log.info("Loaded config!")
                        return 1
                    }
                    val serializer = SUPPORTED_VERSIONS[configVersion]
                    if (serializer != null) {
                        config = ConfigData(json.decodeFromString(serializer, stringData))
                        log.info("Loaded older config!")
                        return 1
                    }
                    throw Error("No supported version found!")
                }
                throw Error("No version found!")
            }
            throw Error("No file found!")
        } catch (e: Exception) {
            log.info("Could not read config file for some reason!")
            log.warn(e.message)
            return -1
        }
    }

    private fun save(config: ConfigData) =
        FileWriter(configFile).use { it.write(json.encodeToString(ConfigData.serializer(), config)) }

    @Serializable
    data class ConfigData(
        @Required val version: Double = 1.0, @Required val bookAuthorName: String = "The Quest Master"
    ) : ConfData {
        constructor(data: ConfData?) : this()
    }

    interface ConfData
}