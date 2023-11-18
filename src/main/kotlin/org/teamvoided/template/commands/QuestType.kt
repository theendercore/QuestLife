package org.teamvoided.template.commands

import net.minecraft.util.BlockRotation
import net.minecraft.util.StringIdentifiable

enum class QuestType : StringIdentifiable {
    Major,
    Minor,
    Creator;

    override fun asString(): String = this.name.lowercase()

    companion object {
        val Codec: StringIdentifiable.EnumCodec<QuestType> =
            StringIdentifiable.createCodec { QuestType.entries.toTypedArray() }
    }
}