package db.repository

import cinterop.NativeSqlite3
import model.DirectoryRow

class DirectoryRepository(private val sql: NativeSqlite3) : Repository<DirectoryRow> {
    override fun all(): List<DirectoryRow> {
        return sql.execMany("SELECT id, path FROM directories;").map {
            DirectoryRow(id = it[0].toInt(), path = it[1])
        }
    }
}
