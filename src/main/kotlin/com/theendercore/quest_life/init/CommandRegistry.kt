package com.theendercore.quest_life.init

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import com.theendercore.quest_life.QuestLife.id
import com.theendercore.quest_life.commands.QuestArgumentType
import com.theendercore.quest_life.commands.QuestCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CommandRegistry {
    fun init() {
        ArgumentTypeRegistry
            .registerArgumentType(id("quest_type"), QuestArgumentType::class.java, QuestArgumentType.Info())
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { d, c, e ->
            QuestCommand.register(d, c, e)
        })
    }
}