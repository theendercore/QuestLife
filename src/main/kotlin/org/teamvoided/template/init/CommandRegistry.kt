package org.teamvoided.template.init

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import org.teamvoided.template.Template.id
import org.teamvoided.template.commands.QuestArgumentType
import org.teamvoided.template.commands.QuestCommand

object CommandRegistry {
    fun init(){
        ArgumentTypeRegistry
            .registerArgumentType(id("quest_type"), QuestArgumentType::class.java, QuestArgumentType.Info())

        QuestCommand.register()
    }
}