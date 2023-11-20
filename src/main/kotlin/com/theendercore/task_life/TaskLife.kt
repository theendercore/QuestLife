package com.theendercore.task_life

import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import com.theendercore.task_life.init.CommandRegistry

@Suppress("unused")
object TaskLife {
    const val MODID = "template"

    @JvmField
    val LOG = LoggerFactory.getLogger(TaskLife::class.java)

    fun commonInit() {
        LOG.info("Hello from Common")
        TaskDatabaseAccess.init()
        CommandRegistry.init()
    }

    fun clientInit() {
        LOG.info(":)")
    }

    fun id(path: String) = Identifier(MODID, path)
}