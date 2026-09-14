package db.repository

import cinterop.NativeSqlite3
import model.SessionRow
import util.parseEpochRealtime

class SessionRepository(private val sql: NativeSqlite3) : Repository<SessionRow> {
    override fun all(): List<SessionRow> {
        return sql.execMany("SELECT id, start_epoch FROM sessions;").map {
            SessionRow(id = it[0].toInt(), startEpoch = parseEpochRealtime(it[1]))
        }
    }

    fun create(now: String): Int {
        return sql.execReturningId("INSERT INTO sessions (start_epoch) VALUES ('$now') RETURNING id;")
    }
}
