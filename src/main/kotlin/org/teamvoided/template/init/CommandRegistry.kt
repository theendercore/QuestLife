package org.teamvoided.template.init

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import org.teamvoided.template.Template.id
import org.teamvoided.template.commands.TaskArgumentType
import org.teamvoided.template.commands.TaskCommand

object CommandRegistry {
    fun init(){
        ArgumentTypeRegistry
            .registerArgumentType(id("quest_type"), TaskArgumentType::class.java, TaskArgumentType.Info())

        TaskCommand.register()
    }
}