package org.teamvoided.template.commands

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.teamvoided.template.TaskDatabaseAccess


object TaskCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            val taskNode = CommandManager.literal("task").requires { it.hasPermissionLevel(2) }.build()
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
                .executes { this.list(it, null) }
                .build()
            taskNode.addChild(listNode)

            val listTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .executes { this.list(it, TaskArgumentType.getTaskType(it, "type")) }
                .build()
            listNode.addChild(listTypeNode)
        })
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        val type = TaskArgumentType.getTaskType(context, "type")
        val task = MessageArgumentType.getMessage(context, "task").string

        val error = TaskDatabaseAccess.add(type, task)
        if (error.isPresent) {
            source.sendError(Text.of("Error:"))
            source.sendError(Text.of(error.get()))
            return 0
        }

        source.sendSystemMessage(Text.of("New $type task added!"))
        return 1
    }

    private fun generate(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.player
        val type = TaskArgumentType.getTaskType(context, "type")
        if (player == null) {
            source.sendError(Text.of("Command Must be run by Player"))
            return 0
        }

        val result = TaskDatabaseAccess.get(type, true)
        if (result.error.isPresent) {
            source.sendError(Text.of("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!

        val book = Items.WRITTEN_BOOK.defaultStack //book
        book.setSubNbt("author", NbtString.of("The Task Master"))
        book.setSubNbt("title", NbtString.of("$type Task"))
        val list = NbtList()
        list.add(NbtString.of(Text.Serializer.toJson(Text.literal(data))))
        book.setSubNbt("pages", list)
        source.world.spawnEntity(ItemEntity(source.world, player.x, player.y, player.z, book))

        source.sendSystemMessage(Text.of("Generated Value: $data"))
        return 1
    }

    private fun list(context: CommandContext<ServerCommandSource>, type: TaskType?): Int {
        val source = context.source

        val result = TaskDatabaseAccess.getAll(type)
        if (result.error.isPresent) {
            source.sendError(Text.of("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!
        data.split("\n").forEach {
            source.sendSystemMessage(Text.of(it))
        }
        return 1
    }
}