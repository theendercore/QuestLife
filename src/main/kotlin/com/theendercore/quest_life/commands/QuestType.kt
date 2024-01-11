package com.theendercore.quest_life.commands

import net.minecraft.util.StringIdentifiable
import net.minecraft.util.StringIdentifiable.createCodec

enum class QuestType : StringIdentifiable {
    Easy,
    Normal,
    Hard,
    Custom,
    Special;

    override fun asString(): String = this.name.lowercase()

    companion object {
        val Codec: StringIdentifiable.EnumCodec<QuestType> = createCodec { QuestType.entries.toTypedArray() }
    }
}