package com.theendercore.quest_life.commands

import com.google.common.base.Supplier
import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.theendercore.quest_life.QuestDatabaseManager
import com.theendercore.quest_life.QuestLife.ModDir
import com.theendercore.quest_life.commands.QuestArgumentType.questTypeArg
import com.theendercore.quest_life.config.QuestConfig
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
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
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


object QuestCommand {
    @Suppress("UNUSED_PARAMETER")
    fun register(
        dispatcher: CommandDispatcher<ServerCommandSource>, context: CommandBuildContext,
        env: CommandManager.RegistrationEnvironment
    ) {

        // /quest
        val questNode = literal("quest").requires { it.hasPermissionLevel(2) }.build()
        dispatcher.root.addChild(questNode)


        // /quest add
        val addNode = literal("add").build()
        questNode.addChild(addNode)
        // /quest add *type*
        val addTypeNode = questTypeArg("type").build()
        addNode.addChild(addTypeNode)
        // /quest add *type* *quest*
        val addQuestNode = argument("quest", MessageArgumentType.message())
            .executes { threaded { add(it) } }
            .build()
        addTypeNode.addChild(addQuestNode)


        // /quest generate
        val generateNode = literal("generate").build()
        questNode.addChild(generateNode)
        // /quest generate *type*
        val genTypeNode = questTypeArg("type")
            .executes { threaded { generate(it, QuestArgumentType.getQuestType(it, "type"), null) } }
            .build()

        generateNode.addChild(genTypeNode)
        // /quest generate *type* player
        val getPlayerTargetNode = literal("player").build()
        genTypeNode.addChild(getPlayerTargetNode)
        // /quest generate *type* player *target*
        val genTargetNode = argument("target", EntityArgumentType.players())
            .executes {
                threaded {
                    generate(
                        it, QuestArgumentType.getQuestType(it, "type"),
                        EntityArgumentType.getOptionalPlayers(it, "target")
                    )
                }
            }
            .build()
        getPlayerTargetNode.addChild(genTargetNode)
        // /quest generate *type* player *target* *title*
        val genTargetNodeTitleArg = argument("title", MessageArgumentType.message())
            .executes {
                threaded {
                    generate(
                        it, QuestArgumentType.getQuestType(it, "type"),
                        EntityArgumentType.getOptionalPlayers(it, "target"),
                        MessageArgumentType.getMessage(it,"title").string
                    )
                }
            }
            .build()
        genTargetNode.addChild(genTargetNodeTitleArg)

        // /quest generate *type* location
        val getLocationTargetNode = literal("location").build()
        genTypeNode.addChild(getLocationTargetNode)
        // /quest generate *type* location *target*
        val genTarget2Node = argument("target", BlockPosArgumentType.blockPos())
            .executes {
                threaded {
                    generateLocation(
                        it, QuestArgumentType.getQuestType(it, "type"),
                        BlockPosArgumentType.getBlockPos(it, "target")
                    )
                }
            }
            .build()
        getLocationTargetNode.addChild(genTarget2Node)
        // /quest generate *type* location *target* *title*
        val genTarget2NodeTitleArg = argument("title", MessageArgumentType.message())
            .executes {
                threaded {
                    generateLocation(
                        it, QuestArgumentType.getQuestType(it, "type"),
                        BlockPosArgumentType.getBlockPos(it, "target"),
                        MessageArgumentType.getMessage(it,"title").string
                    )
                }
            }
            .build()
        genTarget2Node.addChild(genTarget2NodeTitleArg)


        // /quest list
        val listNode = literal("list").executes { threaded { list(it, null) } }.build()
        questNode.addChild(listNode)
        // /quest list *type*
        val listTypeArgNode = questTypeArg("type")
            .executes { threaded { list(it, QuestArgumentType.getQuestType(it, "type")) } }
            .build()
        listNode.addChild(listTypeArgNode)


        // /quest get
        val getNode = literal("get").build()
        questNode.addChild(getNode)
        // /quest get *id*
        val getIdArgNode = argument("id", IntegerArgumentType.integer(1))
            .executes { threaded { getQuest(it, IntegerArgumentType.getInteger(it, "id")) } }
            .build()
        getNode.addChild(getIdArgNode)


        // /quest delete
        val deleteNode = literal("delete").build()
        questNode.addChild(deleteNode)
        // /quest delete *id*
        val deleteIdArgNode = argument("id", IntegerArgumentType.integer(1))
            .executes { threaded { deleteQuest(it, IntegerArgumentType.getInteger(it, "id")) } }
            .build()
        deleteNode.addChild(deleteIdArgNode)


        // /quest export
        val exportNode = literal("export").executes { threaded { export(it) } }.build()
        questNode.addChild(exportNode)


        // /quest import
        val importNode = literal("import").executes { threaded { import(it, false) } }.build()
        questNode.addChild(importNode)
        // /quest import *with_times_used*
        val importTimesArgNode = argument("with_times_used", BoolArgumentType.bool())
            .executes { threaded { import(it, BoolArgumentType.getBool(it, "with_times_used")) } }
            .build()
        importNode.addChild(importTimesArgNode)


        // /quest delete_all
        val deleteAllNode = literal("delete_all")
            .executes { threaded { deleteAll(it, true) } }
            .build()
        questNode.addChild(deleteAllNode)
        // /quest delete_all *backup*
        val deleteAllBackupArgNode = argument("backup", BoolArgumentType.bool())
            .executes { threaded { deleteAll(it, BoolArgumentType.getBool(it, "backup")) } }
            .build()
        deleteAllNode.addChild(deleteAllBackupArgNode)


        // /quest players
        val playersNode = literal("players").build()
        questNode.addChild(playersNode)

        // /quest players add
        val playerListAddNode = literal("add").build()
        playersNode.addChild(playerListAddNode)
        // /quest players add *player*
        val playerListAddArgNode = argument("player", GameProfileArgumentType.gameProfile())
            .executes { threaded { addPlayers(it, GameProfileArgumentType.getProfileArgument(it, "player")) } }
            .build()
        playerListAddNode.addChild(playerListAddArgNode)

        // /quest players list
        val playerListNode = literal("list").executes { threaded { listPlayers(it) } }.build()
        playersNode.addChild(playerListNode)

        // /quest players remove
        val playerListRemoveNode = literal("remove").build()
        playersNode.addChild(playerListRemoveNode)
        // /quest players remove *player*
        val playerListRemoveArgNode = argument("player", GameProfileArgumentType.gameProfile())
            .executes { threaded { removePlayers(it, GameProfileArgumentType.getProfileArgument(it, "player")) } }
            .build()
        playerListRemoveNode.addChild(playerListRemoveArgNode)

        // /quest players list
        val reloadConfigNode = literal("reload_config").executes(::reloadConfig).build()
        questNode.addChild(reloadConfigNode)
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = QuestArgumentType.getQuestType(context, "type")
        val quest = MessageArgumentType.getMessage(context, "quest").string
        if (source.couldError(QuestDatabaseManager.add(type, quest))) return 0

        source.msg("New $type quest added!")
        return 1
    }

    private fun generate(
        context: CommandContext<ServerCommandSource>, type: QuestType, inPlayers: Collection<ServerPlayerEntity>?, title: String = ""
    ): Int {
        val source = context.source
        var players = inPlayers
        var outInt = players?.size
        var output = "Generated $type Quests for $outInt players!"
        if (inPlayers == null) {
            val player = source.player
            if (player == null) {
                source.sError("Command Must be run by Player")
                return 0
            }
            players = listOf(player)
            output = "Generated $type Quest!"
            outInt = 1
        }

        players!!.forEach { player ->
            val result = QuestDatabaseManager.get(type, true)
            if (source.couldError(result.second)) return@generate 0

            source.genBook(result.first!!, type, source.world, Vec3d(player.x, player.y, player.z), title)
        }

        source.msg(output)
        return outInt!!
    }

    private fun generateLocation(context: CommandContext<ServerCommandSource>, type: QuestType, pos: BlockPos, title: String = ""): Int {
        val source = context.source
        val result = QuestDatabaseManager.get(type, true)
        if (source.couldError(result.second)) return 0

        source.genBook(result.first!!, type, source.world, pos.ofCenter(), title)
        source.msg("Quest spawned at ${pos.x} ${pos.y} ${pos.z}")
        return 1
    }

    private fun ServerCommandSource.genBook(
        iData: String, type: QuestType, world: ServerWorld, pos: Vec3d, title: String = ""
    ) {
        var data = iData
        try {
            if (data.contains("[")) {
                val number = Regex("\\[n:\\d*:\\d*]")
                val dataCleaner = Regex("(\\[n:)|]")

                while (data.contains("[p]")) {
                    val result = QuestDatabaseManager.getPlayer(true)
                    if (this.couldError(result.second)) return

                    val player = this.server.userCache?.getByUuid(UUID.fromString(result.first))?.get()?.name

                    data = data.replaceFirst("[p]", player ?: "[Player Not Found]")
                }


                if (data.contains("[n:")) {
                    val inputs = number.findAll(data).map { x ->
                        val procData = x.value.replace(dataCleaner, "").split(":").map { it.toInt() }
                        println(procData)
                        if (procData[0] > procData[1]) {
                            val err = "Wrong input for Number Template for entry [$data]"
                            this.error(err)
                            throw Error(err)
                        }
                        "${Random.nextInt(procData[0], procData[1] + 1)}"
                    }.toList()
                    data = number.split(data)
                        .reduceIndexed { idx, acc, s -> "$acc${if (idx - 1 < inputs.size) inputs[idx - 1] else ""}$s" }
                }
            }
        } catch (e: NumberFormatException) {
            e.message?.let { this.error(it) }
            throw e
        }

        val book = Items.WRITTEN_BOOK.defaultStack
        book.setSubNbt("author", NbtString.of(QuestConfig.config.bookAuthorName))
        book.setSubNbt("title", NbtString.of(title.ifEmpty { "$type Quest" }))
        val list = NbtList()
        list.add(NbtString.of(Text.Serializer.toJson(Text.literal(data))))
        book.setSubNbt("pages", list)
        val bookEntity = ItemEntity(world, pos.x, pos.y, pos.z, book)
        bookEntity.setVelocity(0.0, 0.0, 0.0)
        world.spawnEntity(bookEntity)
    }

    private fun getQuest(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val result = QuestDatabaseManager.getOne(id)
        if (source.couldError(result.second)) return 0

        val data = result.first!!
        source.msg("Quest [$id] - $data")
        return 1
    }

    private fun deleteQuest(context: CommandContext<ServerCommandSource>, id: Int): Int {
        val source = context.source
        val error = QuestDatabaseManager.deleteOne(id)
        if (source.couldError(error)) return 0

        source.msg("Quest [$id] deleted!")
        return 1
    }

    private fun list(context: CommandContext<ServerCommandSource>, type: QuestType?): Int {
        val source = context.source
        val result = QuestDatabaseManager.getAll(type)
        if (source.couldError(result.second)) return 0

        val data = result.first!!
        data.forEach { source.msg("[${it.id}][${it.type}] - ${it.data}") }
        return 1
    }

    private fun export(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        if (exportToFile(source, "quests")) return 0

        source.msg("Quests exported!")
        return 1
    }

    private fun import(context: CommandContext<ServerCommandSource>, readTimesUsed: Boolean): Int {
        val source = context.source
        var questCount = 0
        val file = Paths.get(ModDir, "quests.csv").toFile()
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
                    val quest = slices[1].trim()
                    var count: String? = null
                    if (readTimesUsed) count = slices[2].trim()
                    val error = QuestDatabaseManager.add(QuestType.valueOf(type.lowercase().cap()), quest, count)
                    if (source.couldError(error)) return 0

                    questCount++
                }
            }
        } catch (e: Error) {
            source.sError("[Error While Importing] $e")
            return 0
        }
        source.msg("$questCount Quests imported!")
        return questCount
    }

    private fun deleteAll(context: CommandContext<ServerCommandSource>, makeBackup: Boolean): Int {
        val source = context.source
        if (makeBackup) {
            if (exportToFile(source, "backup_quests")) return 0
            source.msg("Quests backup successful!")
        }
        val error = QuestDatabaseManager.deleteAll()
        if (source.couldError(error)) return 0

        source.msg("All quests deleted!")
        return 1
    }

    private fun addPlayers(context: CommandContext<ServerCommandSource>, players: Collection<GameProfile>): Int {
        val source = context.source
        var count = 0
        var name = ""
        players.forEach {
            it.id
            if (source.couldError(QuestDatabaseManager.addPlayer(it.id.toString())))
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

    private fun removePlayers(context: CommandContext<ServerCommandSource>, players: Collection<GameProfile>): Int {
        val source = context.source
        var i = 0
        for (player in players) {
            val error = QuestDatabaseManager.deleteOnePlayer(player.id.toString())
            if (source.couldError(error)) return 0
            source.msg("${player.name} removed!")
            ++i
        }
        if (i == 0) source.error("No such player!")
        return i
    }

    private fun listPlayers(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val result = QuestDatabaseManager.getAllPlayers()
        if (source.couldError(result.second)) return 0

        val data = result.first!!
        val pm = source.server.userCache
        data.forEach { source.msg("[${it.first}] - ${pm?.getByUuid(UUID.fromString(it.second))?.get()?.name}") }
        return 1
    }

    private fun reloadConfig(context: CommandContext<ServerCommandSource>): Int {
        Thread {
            when (QuestConfig.load()) {
                1 -> context.source.msg("Config reloaded!")
                0 -> context.source.msg("No config was found! Created a new one.")
                else -> context.source.sError("Could not load config file! Check the log for more info.")
            }
        }.start()
        return 1
    }

    private fun exportToFile(src: ServerCommandSource, fileName: String): Boolean {
        val result = QuestDatabaseManager.getAll(null)
        if (src.couldError(result.second)) return true

        var fileContent = "TYPE,QUEST,TIMES_USED\n"
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
    private fun ServerCommandSource.sError(str: String) = this.sendError(Text.translatable(str))
    private fun ServerCommandSource.msg(str: String) = this.sendFeedback({ Text.translatable(str) }, false)

    private fun ServerCommandSource.couldError(error: Optional<String>) =
        if (error.isPresent) {
            this.error(error.get()); true
        } else false

    private fun threaded(callback: Supplier<Int>): Int {
        Thread { callback.get() }.start()
        return 0
    }
}