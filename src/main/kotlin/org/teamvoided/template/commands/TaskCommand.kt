package org.teamvoided.template.commands

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.teamvoided.template.TaskData


object TaskCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            val taskNode = CommandManager.literal("task").requires { it.hasPermissionLevel(4) }.build()
            dispatcher.root.addChild(taskNode)

            val addNode = CommandManager.literal("add")
                .executes { this.add(it) }
                .build()
            taskNode.addChild(addNode)

            val addTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .build()
            addNode.addChild(addTypeNode)

            val addMessageNode = CommandManager
                .argument("task", MessageArgumentType.message())
                .executes { this.add(it) }
                .build()
            addTypeNode.addChild(addMessageNode)


            val generateNode = CommandManager
                .literal("generate")
                .build()
            taskNode.addChild(generateNode)

            val genTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .executes { this.generate(it) }
                .build()
            generateNode.addChild(genTypeNode)


            val listNode = CommandManager
                .literal("list")
                .build()
            taskNode.addChild(listNode)

            val listTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .executes { this.list(it) }
                .build()
            listNode.addChild(listTypeNode)
        })
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        val type = TaskArgumentType.getTaskType(context, "type")
        val task = MessageArgumentType.getMessage(context, "task").string

        val error = TaskData.add(type, task)
        if (error.isPresent){
            source.sendError(Text.of("Error:"))
            source.sendError(Text.of(error.get()))
            return 0
        }

        source.sendSystemMessage(Text.of("New $type task added!"))
        return 1
    }

    private fun generate(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = TaskArgumentType.getTaskType(context, "type")

        val result = TaskData.get(type, true)
        if (result.error.isPresent){
            source.sendError(Text.of("Error:"))
            source.sendError(Text.literal(result.error.get()))
            return 0
        }
        val data = result.value!!

        source.sendSystemMessage(Text.of("Generated Value: $data"))
        return 1
    }

    private fun list(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = TaskArgumentType.getTaskType(context, "type")

        val result = TaskData.getAll(type)
        if (result.error.isPresent){
            source.sendError(Text.of("Error:"))
            source.sendError(Text.literal(result.error.get()))
            return 0
        }
        val data = result.value!!

        source.sendSystemMessage(Text.of(data))
        return 1
    }
}