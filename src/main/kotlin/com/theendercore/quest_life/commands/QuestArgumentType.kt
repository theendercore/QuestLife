package com.theendercore.quest_life.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.util.concurrent.CompletableFuture

object QuestArgumentType {
    fun questTypeArg(name: String): RequiredArgumentBuilder<ServerCommandSource, String> {
        return CommandManager.argument(name, StringArgumentType.string()).suggests(::listSuggestions)
    }

    @Throws(CommandSyntaxException::class)
    fun getQuestType(context: CommandContext<ServerCommandSource>, name: String): QuestType {
        val string = context.getArgument(name, String::class.java)
        try {
            return QuestType.valueOf(string)
        } catch (e: IllegalArgumentException) {
            throw UNKNOWN_QUEST_TYPE_EXCEPTION.create(string)
        }
    }

    private fun <S> listSuggestions(
        commandContext: CommandContext<S>, suggestionsBuilder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return if (commandContext.source is CommandSource) CommandSource.suggestMatching(
            QuestType.entries.map { it.toString() }, suggestionsBuilder
        ) else Suggestions.empty()
    }

    private val UNKNOWN_QUEST_TYPE_EXCEPTION =
        DynamicCommandExceptionType { Text.translatable("Quest type %s not found!", it) }

}