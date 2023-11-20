package com.theendercore.task_life.commands

import net.minecraft.util.StringIdentifiable

enum class TaskType : StringIdentifiable {
    Major,
    Minor,
    Creator;

    override fun asString(): String = this.name.lowercase()

    companion object {
        val Codec: StringIdentifiable.EnumCodec<TaskType> =
            StringIdentifiable.createCodec { TaskType.entries.toTypedArray() }
    }
}