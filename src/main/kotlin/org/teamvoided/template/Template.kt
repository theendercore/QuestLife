package org.teamvoided.template

import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import org.teamvoided.template.init.CommandRegistry

@Suppress("unused")
object Template {
    const val MODID = "template"

    @JvmField
    val LOG = LoggerFactory.getLogger(Template::class.java)

    fun commonInit() {
        LOG.info("Hello from Common")
        TaskDatabaseAccess.init()
        CommandRegistry.init()
    }

    fun clientInit() {
        LOG.info("Hello from Client")
    }

    fun id(path: String) = Identifier(MODID, path)
    fun mc(path: String) = Identifier(path)
}