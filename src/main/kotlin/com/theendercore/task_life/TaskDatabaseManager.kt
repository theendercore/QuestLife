package com.theendercore.task_life

import com.theendercore.task_life.TaskLife.LOG
import com.theendercore.task_life.TaskLife.ModDir
import com.theendercore.task_life.commands.TaskType
import java.nio.file.Paths
import java.sql.*
import java.util.*


object TaskDatabaseManager {
    private val DB_PATH: String = Paths.get(ModDir, "task_data.db").toString()

    fun init() {
        connect {
            it.executeUpdate("create table if not exists tasks (id integer primary key, type string, data string, times_used integer)")
            it.executeUpdate("create table if not exists players (id integer primary key, uuid string, times_used integer)")
        }
    }

    fun add(type: TaskType, task: String, timesUsed: String? = null): Optional<String> {
        try {
            connect { it.executeUpdate("insert into tasks values(null, '$type', '$task', ${timesUsed ?: "0"})") }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.empty()
    }

    fun addPlayer(uuid: String, timesUsed: String? = null): Optional<String> {
        try {
            connect { it.executeUpdate("insert into players values(null, '$uuid', ${timesUsed ?: "0"})") }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.empty()
    }

    fun get(type: TaskType, shouldIncrement: Boolean): Pair<String?, Optional<String>> {
        var value = ""
        try {
            connect {
                val result: ResultSet = it.executeQuery("select * from tasks where type='$type'")
                var taskList = mutableListOf<QueryTask>()
                var max = 0
                var min = 0
                var x = true
                while (result.next()) {
                    val id = result.getInt("id")
                    val type1 = TaskType.valueOf(result.getString("type"))
                    val data = result.getString("data")
                    val timesUsed = result.getInt("times_used")
                    taskList.add(QueryTask(id, type1, data, timesUsed))
                    if (x) {
                        min = timesUsed
                        x = false
                    }
                    if (timesUsed < min) min = timesUsed
                    if (timesUsed > max) max = timesUsed

                }
                if (taskList.isNotEmpty()) {
                    taskList = taskList.filter { task -> (min == max) || task.timesUsed == min }
                        .toMutableList()
                    taskList.shuffle()
                    val task = taskList[0]
                    value = task.data

                    if (shouldIncrement)
                        it.executeUpdate("update tasks set times_used=${task.timesUsed + 1} where id=${task.id}")
                }

            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value == "") Pair(null, Optional.of("No Tasks"))
        else return Pair(value, Optional.empty())
    }

    fun getAll(type: TaskType?): Pair<List<QueryTask>?, Optional<String>> {
        val value = mutableListOf<QueryTask>()
        try {
            connect {
                val result: ResultSet =
                    it.executeQuery("select * from tasks ${if (type != null) "where type='$type'" else ""}")
                while (result.next()) {
                    value.add(
                        QueryTask(
                            result.getInt("id"), TaskType.valueOf(result.getString("type")),
                            result.getString("data"), result.getInt("times_used")
                        )
                    )
                }
            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value.isEmpty()) Pair(null, Optional.of("No Tasks"))
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
            LOG.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value.isEmpty()) Pair(null, Optional.of("No Players"))
        else Pair(value, Optional.empty())
    }

    fun getOne(id: Int): Pair<String?, Optional<String>> {
        var value = ""
        try {
            connect {
                val result: ResultSet = it.executeQuery("select * from tasks where id=$id")
                while (result.next()) {
                    value += "(Type=${TaskType.valueOf(result.getString("type"))}, " +
                            "TimesUsed=${result.getInt("times_used")}, " +
                            "Task=${result.getString("data")})"
                }
            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Pair(null, Optional.of(e.toString()))
        }
        return if (value == "") Pair(null, Optional.of("Could not find Task [$id]"))
        else return Pair(value, Optional.empty())
    }

    fun deleteOne(id: Int): Optional<String> {
        var success = 1
        try {
            connect { success = it.executeUpdate("delete from tasks where id=$id") }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.ofNullable(if (success == 0) "Task [$id] doesn't exists!" else null)
    }

    fun deleteAll(): Optional<String> {
        var success = 1
        try {
            connect { success = it.executeUpdate("delete from task") }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.ofNullable(if (success == 0) "No tasks exists!" else null)
    }

    private fun connect(callback: (statement: Statement) -> Unit) {
        var connection: Connection? = null
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:$DB_PATH")
            val statement: Statement = connection.createStatement()
            statement.queryTimeout = 30
            callback(statement)
        } catch (e: SQLException) {
            LOG.error("[Connect Error] $e")
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                LOG.error("[Close Error] $e")
            }
        }
    }

    data class QueryTask(val id: Int, val type: TaskType, val data: String, val timesUsed: Int) {
        fun toCsvString(): String = "$type,$data,$timesUsed"
    }
}