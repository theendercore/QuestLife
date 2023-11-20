package org.teamvoided.template

import net.fabricmc.loader.api.FabricLoader
import org.teamvoided.template.Template.LOG
import org.teamvoided.template.commands.TaskType
import java.nio.file.Paths
import java.sql.*
import java.util.*


object TaskDatabaseAccess {
    fun init() {
        connect {
            it.executeUpdate("create table if not exists task (id integer primary key, type string, data string, times_used integer)")
        }
    }

    fun add(type: TaskType, task: String): Optional<String> {
        try {
            connect {
                it.executeUpdate("insert into task values(null, '$type', '$task', 0)")
            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Optional.of(e.toString())
        }
        return Optional.empty()
    }

    fun get(type: TaskType, shouldIncrement: Boolean): Errorable<String> {
        var value = ""
        try {
            connect {
                val result: ResultSet = it.executeQuery("select * from task where type='$type'")
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

                    if (shouldIncrement) LOG.info(
                        "Query update: {}",
                        it.executeUpdate("update task set times_used=${task.timesUsed + 1} where id=${task.id}")
                    )
                }

            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Errorable(null, Optional.of(e.toString()))
        }
        return if (value == "") Errorable(null, Optional.of("No Tasks"))
        else return Errorable(value, Optional.empty())
    }

    fun getAll(type: TaskType?): Errorable<String> {
        var value = ""
        try {
            connect {
                val result: ResultSet =
                    it.executeQuery("select * from task ${if (type != null) "where type='$type'" else ""}")
                while (result.next()) {
                    value += "\n[${result.getInt("id")}][${result.getString("type")}] - ${result.getString("data")}"
                }
            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Errorable(null, Optional.of(e.toString()))
        }
        return if (value == "") Errorable(null, Optional.of("No Tasks"))
        else Errorable(value.removePrefix("\n"), Optional.empty())
    }

    fun getOne(id: Int): Errorable<String> {
        var value = ""
        try {
            connect {
                val sql = "select * from task where id=$id"
                LOG.info(sql)
                val result: ResultSet = it.executeQuery(sql)
                while (result.next()) {
                    value += "(Type=${TaskType.valueOf(result.getString("type"))}, " +
                            "TimesUsed=${result.getInt("times_used")}, " +
                            "Task=${result.getString("data")})"
                }
            }
        } catch (e: Error) {
            LOG.error(e.toString())
            return Errorable(null, Optional.of(e.toString()))
        }
        return if (value == "") Errorable(null, Optional.of("Could not find Task [$id]"))
        else return Errorable(value, Optional.empty())
    }

    private fun connect(callback: (statement: Statement) -> Unit) {
        var connection: Connection? = null
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:${dpPath()}")
            val statement: Statement = connection.createStatement()
            statement.queryTimeout = 30
            callback(statement)
        } catch (e: SQLException) {
            LOG.error("[Connect Error]")
            LOG.error(e.message)
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                LOG.error("[Close Error]")
                LOG.error(e.message)
            }
        }
    }

    private fun dpPath(): String =
        Paths.get(FabricLoader.getInstance().gameDir.toString(), "data", "task_data.db").toString()

    data class Errorable<T>(val value: T?, val error: Optional<String>)
    data class QueryTask(val id: Int, val type: TaskType, val data: String, val timesUsed: Int)
}