package com.theendercore.quest_life

import com.theendercore.quest_life.QuestLife.ModDir
import com.theendercore.quest_life.QuestLife.log
import com.theendercore.quest_life.commands.QuestType
import java.nio.file.Paths
import java.sql.*
import java.util.*


object QuestDatabaseManager {
    private val DB_PATH: String = Paths.get(ModDir, "quest_data.db").toString()

    fun init() {
        connect {
            it.executeUpdate("create table if not exists quests (id integer primary key, type string, data string, times_used integer)")
            it.executeUpdate("create table if not exists players (id integer primary key, uuid string, times_used integer)")
        }
    }

    fun add(type: QuestType, quest: String, timesUsed: String? = null): Optional<String> {
        try {
            connect { it.executeUpdate("insert into quests values(null, '$type', '$quest', ${timesUsed ?: "0"})") }
        } catch (e: Error) {
            log.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.empty()
    }

    fun addPlayer(uuid: String, timesUsed: String? = null): Optional<String> {
        try {
            connect { it.executeUpdate("insert into players values(null, '$uuid', ${timesUsed ?: "0"})") }
        } catch (e: Error) {
            log.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.empty()
    }

    fun get(type: QuestType, shouldIncrement: Boolean): Pair<String?, Optional<String>> {
        var value = ""
        try {
            connect { stmt ->
                val result: ResultSet = stmt.executeQuery("select * from quests where type='$type'")
                var questList = mutableListOf<QueryQuest>()
                var max = 0
                var min = 0
                var x = true
                while (result.next()) {
                    val id = result.getInt("id")
                    val type1 = QuestType.valueOf(result.getString("type"))
                    val data = result.getString("data")
                    val timesUsed = result.getInt("times_used")
                    questList.add(QueryQuest(id, type1, data, timesUsed))
                    if (x) {
                        min = timesUsed
                        x = false
                    }
                    if (timesUsed < min) min = timesUsed
                    if (timesUsed > max) max = timesUsed

                }
                if (questList.isNotEmpty()) {
                    questList = questList.filter { (min == max) || it.timesUsed == min }.toMutableList()
                    val quest = questList.random()
                    value = quest.data

                    if (shouldIncrement)
                        stmt.executeUpdate("update quests set times_used=${quest.timesUsed + 1} where id=${quest.id}")
                }

            }
        } catch (e: Error) {
            log.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value == "") Pair(null, Optional.of("No Quests"))
        else return Pair(value, Optional.empty())
    }


    fun getPlayer(shouldIncrement: Boolean): Pair<String?, Optional<String>> {
        var value = ""
        try {
            connect { st ->
                val result: ResultSet = st.executeQuery("select * from players")
                var players = mutableListOf<Triple<String, Int, Int>>()
                var max = 0
                var min = 0
                var x = true
                while (result.next()) {
                    val timesUsed = result.getInt("times_used")
                    players.add(Triple(result.getString("uuid"), timesUsed, result.getInt("id")))
                    if (x) {
                        min = timesUsed
                        x = false
                    }
                    if (timesUsed < min) min = timesUsed
                    if (timesUsed > max) max = timesUsed

                }
                if (players.isNotEmpty()) {
                    players = players.filter { (min == max) || it.second == min }.toMutableList()
                    val quest = players.random()
                    value = quest.first

                    if (shouldIncrement)
                        st.executeUpdate("update players set times_used=${quest.second + 1} where id=${quest.third}")
                }

            }
        } catch (e: Error) {
            log.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value == "") Pair(null, Optional.of("No Quests"))
        else return Pair(value, Optional.empty())
    }

    fun getAll(type: QuestType?): Pair<List<QueryQuest>?, Optional<String>> {
        val value = mutableListOf<QueryQuest>()
        try {
            connect {
                val result: ResultSet =
                    it.executeQuery("select * from quests ${if (type != null) "where type='$type'" else ""}")
                while (result.next()) {
                    value.add(
                        QueryQuest(
                            result.getInt("id"), QuestType.valueOf(result.getString("type")),
                            result.getString("data"), result.getInt("times_used")
                        )
                    )
                }
            }
        } catch (e: Error) {
            log.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value.isEmpty()) Pair(null, Optional.of("No Quests"))
        else Pair(value, Optional.empty())
    }

    fun getAllPlayers(): Pair<List<Pair<Int, String>>?, Optional<String>> {
        val value = mutableListOf<Pair<Int, String>>()
        try {
            connect {
                val result: ResultSet = it.executeQuery("select * from players")
                while (result.next())
                    value.add(Pair(result.getInt("id"), result.getString("uuid")))
            }
        } catch (e: Error) {
            log.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value.isEmpty()) Pair(null, Optional.of("No Players"))
        else Pair(value, Optional.empty())
    }

    fun getOne(id: Int): Pair<String?, Optional<String>> {
        var value = ""
        try {
            connect {
                val result: ResultSet = it.executeQuery("select * from quests where id=$id")
                while (result.next()) {
                    value += "(Type=${QuestType.valueOf(result.getString("type"))}, " +
                            "TimesUsed=${result.getInt("times_used")}, " +
                            "Quest=${result.getString("data")})"
                }
            }
        } catch (e: Error) {
            log.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value == "") Pair(null, Optional.of("Could not find Quest [$id]"))
        else return Pair(value, Optional.empty())
    }

    fun deleteOne(id: Int): Optional<String> {
        var success = 1
        try {
            connect { success = it.executeUpdate("delete from quests where id=$id") }
        } catch (e: Error) {
            log.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.ofNullable(if (success == 0) "Quest [$id] doesn't exists!" else null)
    }

    fun deleteOnePlayer(uuid: String): Optional<String> {
        var success = 1
        try {
            println(uuid)
            connect { success = it.executeUpdate("delete from players where uuid='$uuid'") }
        } catch (e: Error) {
            log.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.ofNullable(if (success == 0) "Player [$uuid] doesn't exists!" else null)
    }

    fun deleteAll(): Optional<String> {
        var success = 1
        try {
            connect { success = it.executeUpdate("delete from quests") }
        } catch (e: Error) {
            log.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.ofNullable(if (success == 0) "No quests exists!" else null)
    }

    private fun connect(callback: (statement: Statement) -> Unit) {
        var connection: Connection? = null
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:$DB_PATH")
            val statement: Statement = connection.createStatement()
            statement.queryTimeout = 30
            callback(statement)
        } catch (e: SQLException) {
            log.error("[Connect Error] $e")
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                log.error("[Close Error] $e")
            }
        }
    }

    data class QueryQuest(val id: Int, val type: QuestType, val data: String, val timesUsed: Int) {
        fun toCsvString(): String = "$type,$data,$timesUsed"
    }
}