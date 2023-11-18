package org.teamvoided.template.commands

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text


object QuestCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            val questNode = CommandManager.literal("quest").requires { it.hasPermissionLevel(4) }.build()
            val addNode = CommandManager.literal("add")
                .executes { this.add(it) }
                .build()
            val addTypeNode = CommandManager
                .argument("type", QuestArgumentType())
                .build()
            val addMessageNode = CommandManager
                .argument("quest", MessageArgumentType.message())
                .executes { this.add(it) }
                .build()


            val generateNode = CommandManager
                .literal("generate")
                .build()
            val genTypeNode = CommandManager
                .argument("type", QuestArgumentType())
                .executes { this.generate(it) }
                .build()

            dispatcher.root.addChild(questNode)

            questNode.addChild(addNode)
            addNode.addChild(addTypeNode)
            addTypeNode.addChild(addMessageNode)

            questNode.addChild(generateNode)
            generateNode.addChild(genTypeNode)
        })
    }

    private fun add(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        val type = QuestArgumentType.getQuestType(context, "type")
        val message = MessageArgumentType.getMessage(context, "quest").string

        source.sendSystemMessage(Text.of("$type $message"))
        return 1
    }

    private fun generate(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val type = QuestArgumentType.getQuestType(context, "type")

        source.sendSystemMessage(Text.of("gen $type"))
        return 1
    }
}