package com.theendercore.quest_life.commands

import com.google.gson.JsonObject
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandBuildContext
import net.minecraft.command.argument.ArgumentTypeInfo
import net.minecraft.command.argument.EnumArgumentType
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.command.ServerCommandSource

class QuestArgumentType :
    EnumArgumentType<QuestType>(QuestType.Codec, { QuestType.entries.toTypedArray() }) {

    companion object {
        fun getQuestType(context: CommandContext<ServerCommandSource>, name: String): QuestType =
            context.getArgument(name, QuestType::class.java)
    }
        class Info() : ArgumentTypeInfo<QuestArgumentType, Template>{
            override fun serializeToNetwork(template: Template, buf: PacketByteBuf) {}
            override fun deserializeFromNetwork(buf: PacketByteBuf): Template = Template()
            override fun unpack(type: QuestArgumentType): Template = Template()
            override fun serializeToJson(template: Template, jsonObject: JsonObject) {}

        }

        class Template : ArgumentTypeInfo.Template<QuestArgumentType> {
            override fun instantiate(context: CommandBuildContext): QuestArgumentType = QuestArgumentType()
            override fun type(): ArgumentTypeInfo<QuestArgumentType, Template> = Info()

        }



}