package com.theendercore.quest_life.init

import com.theendercore.quest_life.commands.QuestCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CommandRegistry {
    fun init() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { d, c, e ->
            QuestCommand.register(d, c, e)
        })
    }
}