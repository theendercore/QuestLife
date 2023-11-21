package com.theendercore.task_life.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.theendercore.task_life.TaskDatabaseManager
import com.theendercore.task_life.TaskLife.GameDir
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Paths


object TaskCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            // /task
            val taskNode = CommandManager
                .literal("task")
                .requires { it.hasPermissionLevel(2) }
                .build()
            dispatcher.root.addChild(taskNode)
            // /task add
            val addNode = CommandManager
                .literal("add")
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
            // /task generate *type* player
            val getPlayerTargetNode = CommandManager
                .literal("player")
                .build()
            genTypeNode.addChild(getPlayerTargetNode)
            // /task generate *type* player *target*
            val genTargetNode = CommandManager
                .argument("target", EntityArgumentType.players())
                .executes {
                    generate(
                        it, TaskArgumentType.getTaskType(it, "type"),
                        EntityArgumentType.getOptionalPlayers(it, "target")
                    )
                }
                .build()
            getPlayerTargetNode.addChild(genTargetNode)
            // /task generate *type* location
            val getLocationTargetNode = CommandManager
                .literal("location")
                .build()
            genTypeNode.addChild(getLocationTargetNode)
            // /task generate *type* location *target*
            val genTarget2Node = CommandManager
                .argument("target", BlockPosArgumentType.blockPos())
                .executes {
                    generateLocation(
                        it, TaskArgumentType.getTaskType(it, "type"),
                        BlockPosArgumentType.getBlockPos(it, "target")
                    )
                }
                .build()
            getLocationTargetNode.addChild(genTarget2Node)


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

            // /task export
            val exportNode = CommandManager
                .literal("export")
                .executes { export(it) }
                .build()
            taskNode.addChild(exportNode)

            // /task import
            val importNode = CommandManager
                .literal("import")
                .executes { import(it, false) }
                .build()
            taskNode.addChild(importNode)
            // /task import *with_times_used*
            val importTimesNode = CommandManager
                .argument("with_times_used", BoolArgumentType.bool())
                .executes { import(it, BoolArgumentType.getBool(it, "with_times_used")) }
                .build()
            importNode.addChild(importTimesNode)

            // /task delete_all
            val deleteAllNode = CommandManager
                .literal("delete_all")
                .executes { deleteAll(it, true) }
                .build()
            taskNode.addChild(deleteAllNode)
            // /task delete_all *backup*
            val deleteAllBackupNode = CommandManager
                .argument("backup", BoolArgumentType.bool())
                .executes { deleteAll(it, BoolArgumentType.getBool(it, "backup")) }
                .build()
            deleteAllNode.addChild(deleteAllBackupNode)

        })
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = TaskArgumentType.getTaskType(context, "type")
        val task = MessageArgumentType.getMessage(context, "task").string
        val error = TaskDatabaseManager.add(type, task)
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
        var output = "Generated $type Tasks for $outInt players!"
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
            val result = TaskDatabaseManager.get(type, true)
            if (result.error.isPresent) {
                source.sendError(Text.literal("Error: ${result.error.get()}"))
                return 0
            }
            genBook(result.value!!, type, source.world, Vec3d(player.x, player.y, player.z))
        }

        source.sendSystemMessage(Text.literal(output))
        return outInt!!
    }

    private fun generateLocation(context: CommandContext<ServerCommandSource>, type: TaskType, pos: BlockPos): Int {
        val source = context.source
        val result = TaskDatabaseManager.get(type, true)
        if (result.error.isPresent) {
            source.sendError(Text.literal("Error: ${result.error.get()}"))
            return 0
        }
        genBook(result.value!!, type, source.world, pos.ofCenter())
        source.sendSystemMessage(Text.literal("Task spawned at ${pos.x} ${pos.y} ${pos.z}"))
        return 1
    }

    private fun genBook(data: String, type: TaskType, world: ServerWorld, pos: Vec3d) {
        val book = Items.WRITTEN_BOOK.defaultStack
        book.setSubNbt("author", NbtString.of("The Task Master"))
        book.setSubNbt("title", NbtString.of("$type Task"))
        val list = NbtList()
        list.add(NbtString.of(Text.Serializer.toJson(Text.literal(data))))
        book.setSubNbt("pages", list)
        val bookEntity = ItemEntity(world, pos.x, pos.y, pos.z, book)
        bookEntity.setVelocity(0.0, -0.1,0.0)
        world.spawnEntity(bookEntity)
    }

    private fun getTask(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val result = TaskDatabaseManager.getOne(id)
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
        val error = TaskDatabaseManager.deleteOne(id)
        if (error.isPresent) {
            source.sendError(Text.literal("Error: ${error.get()}"))
            return 0
        }
        source.sendSystemMessage(Text.literal("Task [$id] deleted!"))
        return 1
    }

    private fun list(context: CommandContext<ServerCommandSource>, type: TaskType?): Int {
        val source = context.source
        val result = TaskDatabaseManager.getAll(type)
        if (result.error.isPresent) {
            source.sendError(Text.literal("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!
        data.forEach { source.sendSystemMessage(Text.literal("[${it.id}][${it.type}] - ${it.data}")) }
        return 1
    }

    private fun export(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val result = TaskDatabaseManager.getAll(null)
        if (result.error.isPresent) {
            source.sendError(Text.literal("Error: ${result.error.get()}"))
            return 0
        }
        val data = result.value!!
        var exportString = "TYPE,TASK,TIMES_USED\n"
        data.forEach { exportString += it.toCsvString() + "\n" }
        val exportFile =
            FileWriter(Paths.get(Paths.get(GameDir, "export").toString(), "tasks.csv").toFile())
        exportFile.write(exportString)
        exportFile.close()

        source.sendSystemMessage(Text.literal("Tasks exported!"))
        return 1
    }

    private fun import(context: CommandContext<ServerCommandSource>, readTimesUsed: Boolean): Int {
        val source = context.source
        var taskCount = 0
        val file = Paths.get(Paths.get(GameDir, "import").toString(), "tasks.csv").toFile()
        if (!file.exists()) {
            source.sendError(Text.literal("File doesn't exist!"))
            return 0
        }
        val importFile = BufferedReader(FileReader(file))
        try {
            for (it in importFile.lines()) {
                if (it.contains(",")) {
                    val slices = it.replace(Char(65279), Char(32)).split(",")
                    val type = slices[0].trim()
                    if (type == "" || type.lowercase() == "type") continue
                    val task = slices[1].trim()
                    var count: String? = null
                    if (readTimesUsed) count = slices[2].trim()
                    val error = TaskDatabaseManager.add(TaskType.valueOf(type.lowercase().cap()), task, count)
                    if (error.isPresent) {
                        source.sendError(Text.literal("Error: ${error.get()}"))
                        return 0
                    }
                    taskCount++
                }
            }
        } catch (e: Error) {
            source.sendError(Text.literal("[Error While Importing] $e"))
            return 0
        }
        source.sendSystemMessage(Text.literal("$taskCount Tasks imported!"))
        return taskCount
    }

    private fun deleteAll(context: CommandContext<ServerCommandSource>, makeBackup: Boolean): Int {
        val source = context.source
        if (makeBackup) {
            val result = TaskDatabaseManager.getAll(null)
            if (result.error.isPresent) {
                source.sendError(Text.literal("Error: ${result.error.get()}"))
                return 0
            }
            val data = result.value!!
            var exportString = "TYPE,TASK,TIMES_USED\n"
            data.forEach { exportString += it.toCsvString() + "\n" }
            val exportFile =
                FileWriter(Paths.get(Paths.get(GameDir, "export").toString(), "backup_tasks.csv").toFile())
            exportFile.write(exportString)
            exportFile.close()
        }
        val error = TaskDatabaseManager.deleteAll()
        if (error.isPresent) {
            source.sendError(Text.literal("Error: ${error.get()}"))
            return 0
        }
        source.sendSystemMessage(Text.literal("All tasks deleted!"))
        return 1
    }

    private fun String.cap(): String = this.replaceFirstChar { it.uppercaseChar() }
}