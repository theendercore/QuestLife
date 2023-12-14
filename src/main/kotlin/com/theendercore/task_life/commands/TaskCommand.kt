package com.theendercore.task_life.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.theendercore.task_life.TaskDatabaseManager
import com.theendercore.task_life.TaskLife.ModDir
import net.minecraft.command.CommandBuildContext
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.GameProfileArgumentType
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
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import kotlin.random.Random


object TaskCommand {
    @Suppress("UNUSED_PARAMETER")
    fun register(
        dispatcher: CommandDispatcher<ServerCommandSource>, context: CommandBuildContext,
        env: CommandManager.RegistrationEnvironment
    ) {

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
        val listTypeArgNode = CommandManager
            .argument("type", TaskArgumentType())
            .executes { list(it, TaskArgumentType.getTaskType(it, "type")) }
            .build()
        listNode.addChild(listTypeArgNode)


        // /task get
        val getNode = CommandManager
            .literal("get")
            .build()
        taskNode.addChild(getNode)
        // /task get *id*
        val getIdArgNode = CommandManager
            .argument("id", IntegerArgumentType.integer(1))
            .executes { getTask(it, IntegerArgumentType.getInteger(it, "id")) }
            .build()
        getNode.addChild(getIdArgNode)


        // /task delete
        val deleteNode = CommandManager
            .literal("delete")
            .build()
        taskNode.addChild(deleteNode)
        // /task delete *id*
        val deleteIdArgNode = CommandManager
            .argument("id", IntegerArgumentType.integer(1))
            .executes { deleteTask(it, IntegerArgumentType.getInteger(it, "id")) }
            .build()
        deleteNode.addChild(deleteIdArgNode)

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
        val importTimesArgNode = CommandManager
            .argument("with_times_used", BoolArgumentType.bool())
            .executes { import(it, BoolArgumentType.getBool(it, "with_times_used")) }
            .build()
        importNode.addChild(importTimesArgNode)


        // /task delete_all
        val deleteAllNode = CommandManager
            .literal("delete_all")
            .executes { deleteAll(it, true) }
            .build()
        taskNode.addChild(deleteAllNode)
        // /task delete_all *backup*
        val deleteAllBackupArgNode = CommandManager
            .argument("backup", BoolArgumentType.bool())
            .executes { deleteAll(it, BoolArgumentType.getBool(it, "backup")) }
            .build()
        deleteAllNode.addChild(deleteAllBackupArgNode)


        // /task players
        val playersNode = CommandManager
            .literal("players")
            .build()
        taskNode.addChild(playersNode)

        // /task players add
        val playerListAddNode = CommandManager
            .literal("add")
            .build()
        playersNode.addChild(playerListAddNode)
        // /task players add *player*
        val playerListAddArgNode = CommandManager
            .argument("player", GameProfileArgumentType.gameProfile())
            .executes { addPlayers(it, GameProfileArgumentType.getProfileArgument(it, "player")) }
            .build()
        playerListAddNode.addChild(playerListAddArgNode)

        // /task players list
        val playerListNode = CommandManager
            .literal("list")
            .executes { listPlayers(it) }
            .build()
        playersNode.addChild(playerListNode)
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = TaskArgumentType.getTaskType(context, "type")
        val task = MessageArgumentType.getMessage(context, "task").string
        if (source.couldError(TaskDatabaseManager.add(type, task))) return 0

        source.msg("New $type task added!")
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
                source.sError("Command Must be run by Player")
                return 0
            }
            players = listOf(player)
            output = "Generated $type Task!"
            outInt = 1
        }

        players!!.forEach { player ->
            val result = TaskDatabaseManager.get(type, true)
            if (source.couldError(result.second)) return@generate 0

            genBook(result.first!!, type, source.world, Vec3d(player.x, player.y, player.z), source)
        }

        source.msg(output)
        return outInt!!
    }

    private fun generateLocation(context: CommandContext<ServerCommandSource>, type: TaskType, pos: BlockPos): Int {
        val source = context.source
        val result = TaskDatabaseManager.get(type, true)
        if (source.couldError(result.second)) return 0

        genBook(result.first!!, type, source.world, pos.ofCenter(), source)
        source.msg("Task spawned at ${pos.x} ${pos.y} ${pos.z}")
        return 1
    }

    private fun genBook(iData: String, type: TaskType, world: ServerWorld, pos: Vec3d, src: ServerCommandSource) {
        var data = iData
        println("BookGen-DEBUG")
        val percent = Regex("\\[%:\\d{1,2}:((100)|(\\d{1,2}))]")
        val time = Regex("\\[t:([1-5]?[0-9]):([1-5]?[0-9]|60):[smhd]]")
        val dataCleaner = Regex("(\\[((%)|(t)):)|]")

        if (data.contains("[")) {
            while (data.contains("[p]")) {
                data = data.replaceFirst("[p]", "ENDER_END")
            }

            if (data.contains("[%")) {
                val inputs = percent.findAll(data).map { x ->
                    val procData = x.value.replace(dataCleaner, "").split(":").map { it.toInt() }
                    if (procData[0] > procData[1]) {
                        val err = "Wrong input Percent Template for [$data]"
                        src.error(err)
                        throw Error(err)
                    }
                    "${Random.nextInt(procData[0], procData[1] + 1)}%"
                }.toList()
                data = percent.split(data)
                    .reduceIndexed { idx, acc, s -> "$acc${if (idx - 1 < inputs.size) inputs[idx - 1] else ""}$s" }
            }

            if (data.contains("[t")) {
                val inputs = time.findAll(data).map {
                    val procData = it.value.replace(dataCleaner, "").split(":")
                    if (procData[0].toInt() > procData[1].toInt()) {
                        val err = "Wrong input Time Template for [$data]"
                        src.error(err)
                        throw Error(err)
                    }
                    val x = when (procData[2]) {
                        "s" -> "seconds"
                        "m" -> "minutes"
                        "h" -> "hours"
                        "d" -> "days"
                        else -> throw Error("Time regex failed and caught and extra value or didn't catch one at all!")
                    }
                    "${Random.nextInt(procData[0].toInt(), procData[1].toInt() + 1)} $x"
                }.toList()
                data = time.split(data)
                    .reduceIndexed { idx, acc, s -> "$acc${if (idx - 1 < inputs.size) inputs[idx - 1] else ""} $s" }
            }
        }


        val book = Items.WRITTEN_BOOK.defaultStack
        book.setSubNbt("author", NbtString.of("The Task Master"))
        book.setSubNbt("title", NbtString.of("$type Task"))
        val list = NbtList()
        list.add(NbtString.of(Text.Serializer.toJson(Text.literal(data))))
        book.setSubNbt("pages", list)
        val bookEntity = ItemEntity(world, pos.x, pos.y, pos.z, book)
        bookEntity.setVelocity(0.0, -0.1, 0.0)
        world.spawnEntity(bookEntity)
    }

    private fun getTask(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val result = TaskDatabaseManager.getOne(id)
        if (source.couldError(result.second)) return 0

        val data = result.first!!
        source.msg("Task [$id] - $data")
        return 1
    }

    private fun deleteTask(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val error = TaskDatabaseManager.deleteOne(id)
        if (source.couldError(error)) return 0

        source.msg("Task [$id] deleted!")
        return 1
    }

    private fun list(context: CommandContext<ServerCommandSource>, type: TaskType?): Int {
        val source = context.source
        val result = TaskDatabaseManager.getAll(type)
        if (source.couldError(result.second)) return 0

        val data = result.first!!
        data.forEach { source.msg("[${it.id}][${it.type}] - ${it.data}") }
        return 1
    }

    private fun export(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        if (exportToFile(source, "tasks")) return 0

        source.msg("Tasks exported!")
        return 1
    }

    private fun import(context: CommandContext<ServerCommandSource>, readTimesUsed: Boolean): Int {
        val source = context.source
        var taskCount = 0
        val file = Paths.get(ModDir, "tasks.csv").toFile()
        if (!file.exists()) {
            source.error("File doesn't exist!")
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
                    if (source.couldError(error)) return 0

                    taskCount++
                }
            }
        } catch (e: Error) {
            source.sendError(Text.literal("[Error While Importing] $e"))
            return 0
        }
        source.msg("$taskCount Tasks imported!")
        return taskCount
    }

    private fun deleteAll(context: CommandContext<ServerCommandSource>, makeBackup: Boolean): Int {
        val source = context.source
        if (makeBackup) {
            if (exportToFile(source, "backup_tasks")) return 0
            source.msg("Tasks backup successful!")
        }
        val error = TaskDatabaseManager.deleteAll()
        if (source.couldError(error)) return 0

        source.msg("All tasks deleted!")
        return 1
    }

    private fun addPlayers(context: CommandContext<ServerCommandSource>, players: Collection<GameProfile>): Int {
        val source = context.source
        var count = 0
        var name = ""
        players.forEach {
            it.id
            if (source.couldError(TaskDatabaseManager.addPlayer(it.id.toString())))
                return@addPlayers 0
            count++
            name = it.name
        }

        val output = if (count > 1) Pair("New $count players added!", count)
        else if (count == 0) Pair("No new players added", 0)
        else Pair("New player added ($name)!", 1)

        source.msg(output.first)
        return output.second
    }

    private fun listPlayers(context: CommandContext<ServerCommandSource> ): Int {
        val source = context.source
        val result = TaskDatabaseManager.getAllPlayers()
        if (source.couldError(result.second)) return 0

        val data = result.first!!
//        GameProfile()
       val pm = source.server.userCache
        data.forEach { source.msg("[${it.first}] - ${ pm?.getByUuid(UUID.fromString(it.second))?.get()?.name}") }
        return 1
    }


    private fun exportToFile(src: ServerCommandSource, fileName: String): Boolean {
        val result = TaskDatabaseManager.getAll(null)
        if (src.couldError(result.second)) return true

        var fileContent = "TYPE,TASK,TIMES_USED\n"
        result.first!!.forEach { fileContent += it.toCsvString() + "\n" }
        try {
            val file = FileWriter(Paths.get(ModDir, "$fileName.csv").toFile())
            file.write(fileContent)
            file.close()
        } catch (e: IOException) {
            src.error("Failed to write file : $e")
            return true
        }

        return false
    }

    private fun String.cap(): String = this.replaceFirstChar { it.uppercaseChar() }
    private fun ServerCommandSource.error(str: String) = this.sError("Error : $str")
    private fun ServerCommandSource.sError(str: String) = this.sendError(Text.literal(str))
    private fun ServerCommandSource.msg(str: String) = this.sendSystemMessage(Text.literal(str))

    private fun ServerCommandSource.couldError(error: Optional<String>) =
        if (error.isPresent) {
            this.error(error.get()); true
        } else false
}