package com.theendercore.task_life.init

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import com.theendercore.task_life.TaskLife.id
import com.theendercore.task_life.commands.TaskArgumentType
import com.theendercore.task_life.commands.TaskCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CommandRegistry {
    fun init() {
        ArgumentTypeRegistry
            .registerArgumentType(id("quest_type"), TaskArgumentType::class.java, TaskArgumentType.Info())
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { d, c, e ->
            TaskCommand.register(d, c, e)
        })
    }
}