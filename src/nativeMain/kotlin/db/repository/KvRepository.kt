package db.repository

import cinterop.NativeSqlite3
import db.withNSqlite

data class KvRow(
    val key: Int,
    val value: String
)


class KvRepository(private val sql: NativeSqlite3) : Repository<KvRow> {
    override fun all(): List<KvRow> = sql.execMany("SELECT key, value FROM kv").map { KvRow(it[0].toInt(), it[1]) }
    fun select(key: Int) {
        sql
    }
}