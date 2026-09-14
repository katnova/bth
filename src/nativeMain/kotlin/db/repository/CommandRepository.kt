package db.repository

import cinterop.NativeSqlite3
import model.CommandRow

class CommandRepository(private val sql: NativeSqlite3) : Repository<CommandRow> {
    override fun all(): List<CommandRow> {
        return sql.execMany("SELECT id, command FROM commands;").map {
            CommandRow(id = it[0].toInt(), command = it[1])
        }
    }
}