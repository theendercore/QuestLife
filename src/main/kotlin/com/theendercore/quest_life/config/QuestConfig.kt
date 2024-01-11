package com.theendercore.quest_life.config

object QuestConfig {

    val data = ConfigData()

    data class ConfigData(
        val bookAuthorName: String = "The Quest Master",
        val useQuestTypeNames: Boolean = true,
        val bookName: String = "Book Name",
        val x :String = ""
    )
}