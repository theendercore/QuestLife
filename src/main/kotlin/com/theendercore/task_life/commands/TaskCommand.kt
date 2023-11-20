package com.theendercore.task_life.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
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
import com.theendercore.task_life.TaskDatabaseAccess
import com.theendercore.task_life.TaskLife.GameDir
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import java.io.FileWriter
import java.nio.file.Paths


object TaskCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            // /task
            val taskNode = CommandManager.literal("task").requires { it.hasPermissionLevel(2) }.build()
            dispatcher.root.addChild(taskNode)
            // /task add
            val addNode = CommandManager.literal("add")
                .executes { add(it) }
                .build()
            taskNode.addChild(addNode)
            // /task add *type*
            val addTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .build()
            addNode.addChild(addTypeNode)
            // /task add *type* *task*
            val addTaskNode = CommandManager
                .argument("task", MessageArgumentType.message())
                .executes { add(it) }
                .build()
            addTypeNode.addChild(addTaskNode)

            // /task generate
            val generateNode = CommandManager
                .literal("generate")
                .build()
            taskNode.addChild(generateNode)
            // /task generate *type*
            val genTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .executes { generate(it, TaskArgumentType.getTaskType(it, "type"), null) }
                .build()
            generateNode.addChild(genTypeNode)
            // /task generate *type* *target*
            val genTargetNode = CommandManager
                .argument("target", EntityArgumentType.players())
                .executes {
                    generate(
                        it,
                        TaskArgumentType.getTaskType(it, "type"),
                        EntityArgumentType.getOptionalPlayers(it, "target")
                    )
                }
                .build()
            genTypeNode.addChild(genTargetNode)

            // /task list
            val listNode = CommandManager
                .literal("list")
                .executes { list(it, null) }
                .build()
            taskNode.addChild(listNode)
            // /task list *type*
            val listTypeNode = CommandManager
                .argument("type", TaskArgumentType())
                .executes { list(it, TaskArgumentType.getTaskType(it, "type")) }
                .build()
            listNode.addChild(listTypeNode)

            // /task get
            val getNode = CommandManager
                .literal("get")
                .build()
            taskNode.addChild(getNode)
            // /task get *id*
            val getIdNode = CommandManager
                .argument("id", IntegerArgumentType.integer(1))
                .executes { getTask(it, IntegerArgumentType.getInteger(it, "id")) }
                .build()
            getNode.addChild(getIdNode)


            // /task delete
            val deleteNode = CommandManager
                .literal("delete")
                .build()
            taskNode.addChild(deleteNode)
            // /task delete *id*
            val deleteIdNode = CommandManager
                .argument("id", IntegerArgumentType.integer(1))
                .executes { deleteTask(it, IntegerArgumentType.getInteger(it, "id")) }
                .build()
            deleteNode.addChild(deleteIdNode)

//            // /task export
            val exportNode = CommandManager
                .literal("export")
                .executes { export(it) }
                .build()
            taskNode.addChild(exportNode)
        })
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = TaskArgumentType.getTaskType(context, "type")
        val task = MessageArgumentType.getMessage(context, "task").string
        val error = TaskDatabaseAccess.add(type, task)
        if (error.isPresent) {
            source.sendError(Text.literal("Error: ${error.get()}"))
            return 0
        }
        source.sendSystemMessage(Text.literal("New $type task added!"))
        return 1
    }

    private fun generate(
        context: CommandContext<ServerCommandSource>, type: TaskType, inPlayers: Collection<ServerPlayerEntity>?
    ): Int {
        val source = context.source
        var players = inPlayers
        var outInt = players?.size
        var output = "Generated $type Tasks for ${outInt} players!"
        if (inPlayers == null) {
            val player = source.player
            if (player == null) {
                source.sendError(Text.literal("Command Must be run by Player"))
                return 0
            }
            players = listOf(player)
            output = "Generated $type Task!"
            outInt = 1
        }

        players!!.forEach { player ->
            val result = TaskDatabaseAccess.get(type, true)
            if (result.error.isPresent) {
                source.sendError(Text.literal("Error: ${result.error.get()}"))
                return 0
            }
            genBook(result.value!!, type, source.world, player)
        }

        source.sendSystemMessage(Text.literal(output))
        return outInt!!
    }

    private fun genBook(data: String, type: TaskType, world: ServerWorld, player: ServerPlayerEntity) {
        val book = Items.WRITTEN_BOOK.defaultStack
        book.setSubNbt("author", NbtString.of("The Task Master"))
        book.setSubNbt("title", NbtString.of("$type Task"))
        val list = NbtList()
        list.add(NbtString.of(Text.Serializer.toJson(Text.literal(data))))
        book.setSubNbt("pages", list)
        world.spawnEntity(ItemEntity(world, player.x, player.y, player.z, book))
    }

    private fun getTask(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val result = TaskDatabaseAccess.getOne(id)
        if (result.error.isPresent) {
            source.sendError(Text.literal("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!
        source.sendSystemMessage(Text.literal("Task [$id] - $data"))
        return 1
    }

    private fun deleteTask(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val error = TaskDatabaseAccess.deleteOne(id)
        if (error.isPresent) {
            source.sendError(Text.literal("Error: ${error.get()}"))
            return 0
        }
        source.sendSystemMessage(Text.literal("Task [$id] deleted!"))
        return 1
    }

    private fun list(context: CommandContext<ServerCommandSource>, type: TaskType?): Int {
        val source = context.source
        val result = TaskDatabaseAccess.getAll(type)
        if (result.error.isPresent) {
            source.sendError(Text.literal("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!
        data.forEach { source.sendSystemMessage(Text.literal("\n[${it.id}][${it.type}] - ${it.data}")) }
        return 1
    }

    private fun export(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val result = TaskDatabaseAccess.getAll(null)
        if (result.error.isPresent) {
            source.sendError(Text.literal("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!
        var exportString = "TYPE,TASK,TIMES_USED\n"
        data.forEach { exportString += it.toCsvString() + "\n" }
        val path = Paths.get(GameDir, "export").toFile()
        path.mkdirs()
        val exportFile = FileWriter(Paths.get(path.toString(), "tasks.csv").toFile())
        exportFile.write(exportString)
        exportFile.close()

        source.sendSystemMessage(Text.literal("Tasks exported!"))
        return 1
    }

    private fun import(context: CommandContext<ServerCommandSource>, readTimesUsed: Boolean): Int {
        val source = context.source
        return 1
    }
}