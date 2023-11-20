package com.theendercore.task_life

import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import com.theendercore.task_life.init.CommandRegistry
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Paths

@Suppress("unused")
object TaskLife {
    const val MODID = "template"

    @JvmField
    val LOG = LoggerFactory.getLogger(TaskLife::class.java)
    @JvmField
    val GameDir = FabricLoader.getInstance().gameDir.toString()

    fun commonInit() {
        LOG.info("Hello from Common")
        Paths.get(GameDir, "import").toFile().mkdirs()
        TaskDatabaseAccess.init()
        CommandRegistry.init()
    }

    fun clientInit() {
        LOG.info(":)")
    }

    fun id(path: String) = Identifier(MODID, path)
}