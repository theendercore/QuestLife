package com.theendercore.quest_life

import com.theendercore.quest_life.config.QuestConfig
import com.theendercore.quest_life.init.CommandRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths

@Suppress("unused")
object QuestLife {
    const val MODID = "quest_life"

    @JvmField
    val log: Logger = LoggerFactory.getLogger(QuestLife::class.java)

    @JvmField
    val GameDir = FabricLoader.getInstance().gameDir.toString()

    @JvmField
    val ModDir = Paths.get(GameDir, "data", "quest_life").toString()

    fun commonInit() {
        log.info("Hello from Common")
        Paths.get(ModDir).toFile().mkdirs()
        QuestConfig.load()
        QuestDatabaseManager.init()
        CommandRegistry.init()
    }

    fun clientInit() {
        log.info(":)")
    }

    fun id(path: String) = Identifier(MODID, path)
}