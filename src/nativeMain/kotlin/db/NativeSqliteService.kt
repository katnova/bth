package db

import cinterop.NativeSqlite3
import db.repository.BookmarkRepository
import db.repository.CommandRepository
import db.repository.DirectoryRepository
import db.repository.HistoryRepository
import db.repository.SessionRepository
import setting.SettingOption
import setting.Settings.read

/**
 * Thin facade that owns the SQLite connection lifecycle and wires up the per-entity
 * repositories. Cross-entity operations that don't cleanly belong to a single repository
 * ([purge], [tableCounts]) live here.
 */
class NativeSqliteService : Database {
    private val sql: NativeSqlite3 = NativeSqlite3(SettingOption.SQLITE_DB_PATH.read<String>())

    override val commands = CommandRepository(sql)
    override val directories = DirectoryRepository(sql)
    override val sessions = SessionRepository(sql)
    override val history = HistoryRepository(sql)
    override val bookmarks = BookmarkRepository(sql)

    init {
        Schema.createTablesIfNotExists(sql)
    }

    override fun purge() {
        sql.rawExec(
            """
DELETE FROM history;
DELETE FROM commands;
DELETE FROM directories;            
        """.trimIndent()
        )
    }

    override fun tableCounts(): Triple<Long, Long, Long> {
        val res = sql.execMany(
            """
            SELECT 
                (SELECT count(*) FROM commands),
                (SELECT count(*) FROM directories),
                (SELECT count(*) FROM history);
        """.trimIndent()
        )

        return if (res.isEmpty()) {
            Triple(0L, 0L, 0L)
        } else {
            val counts = res[0]
            Triple(counts[0].toLong(), counts[1].toLong(), counts[2].toLong())
        }
    }

    override fun close() {
        sql.close()
    }
}

inline fun withNSqlite(block: (NativeSqliteService) -> Unit) {
    val nSqlite = NativeSqliteService()
    block(nSqlite)
    nSqlite.close()
}