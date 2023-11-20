package com.theendercore.task_life.commands

import com.google.gson.JsonObject
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandBuildContext
import net.minecraft.command.argument.ArgumentTypeInfo
import net.minecraft.command.argument.EnumArgumentType
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.command.ServerCommandSource

class TaskArgumentType :
    EnumArgumentType<TaskType>(TaskType.Codec, { TaskType.entries.toTypedArray() }) {

    companion object {
        fun getTaskType(context: CommandContext<ServerCommandSource>, name: String): TaskType =
            context.getArgument(name, TaskType::class.java)
    }
        class Info() : ArgumentTypeInfo<TaskArgumentType, Template>{
            override fun serializeToNetwork(template: Template, buf: PacketByteBuf) {}
            override fun deserializeFromNetwork(buf: PacketByteBuf): Template = Template()
            override fun unpack(type: TaskArgumentType): Template = Template()
            override fun serializeToJson(template: Template, jsonObject: JsonObject) {}

        }

        class Template : ArgumentTypeInfo.Template<TaskArgumentType> {
            override fun instantiate(context: CommandBuildContext): TaskArgumentType = TaskArgumentType()
            override fun type(): ArgumentTypeInfo<TaskArgumentType, Template> = Info()

        }



}