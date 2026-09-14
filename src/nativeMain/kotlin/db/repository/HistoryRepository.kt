package db.repository

import cinterop.NativeSqlite3
import model.HistoryRow
import model.SqliteQueryBuilder
import util.parseEpochRealtime
import util.sqlEscape

class HistoryRepository(private val sql: NativeSqlite3) : Repository<HistoryRow> {
    private val baseSelectFrom = """
SELECT h.id,
       h.start_epoch,
       sd.path,
       c.command,
       h.elevated_before,
       h.elevated_after,
       ed.path,
       h.exit_code,
       h.end_epoch,
       h.session
FROM history h
         JOIN directories sd ON sd.id = h.start_dir_id
         JOIN commands c ON c.id = h.cmd_id
         LEFT JOIN directories ed ON ed.id = h.end_dir_id
        """.trimIndent()

    private fun parseSqliteResultsToHistoryRow(res: Array<String>): HistoryRow {
        return HistoryRow(
            id = res[0],
            startEpoch = parseEpochRealtime(res[1]),
            startDir = res[2],
            cmd = res[3],
            elevatedBefore = res[4] == "1",
            elevatedAfter = if (res[5].isEmpty()) null else res[5] == "1",
            endDir = res[6].ifEmpty { null },
            exitCode = if (res[7].isEmpty()) null else res[7].toInt(),
            endEpoch = if (res[8].isEmpty()) null else parseEpochRealtime(res[8]),
            session = if (res[9].isEmpty()) -1 else res[9].toInt()
        )
    }

    override fun all(): List<HistoryRow> {
        return sql.execMany(baseSelectFrom).map {
            parseSqliteResultsToHistoryRow(it)
        }
    }

    private fun filterExecMany(
        num: Int,
        offset: Int = 0,
        sessionFilterId: Int = -1,
        block: (SqliteQueryBuilder) -> SqliteQueryBuilder,
    ): List<HistoryRow> {
        val builder = SqliteQueryBuilder(baseSelectFrom)
        return sql.execMany(block(builder).also {
            if (sessionFilterId != -1) {
                it.whereEquals("h.session", sessionFilterId.toString())
            }
        }.orderBy("h.id DESC").limit(num).offset(offset).build()).map {
                parseSqliteResultsToHistoryRow(it)
            }.reversed()
    }

    fun filter(filter: String, num: Int, offset: Int = 0, sessionFilterId: Int): List<HistoryRow> = filterExecMany(
        num = num, offset = offset, sessionFilterId = sessionFilterId
    ) { builder ->
        builder.whereLikeContains("c.command", filter.sqlEscape())
    }

    fun filterPwd(filter: String, num: Int, offset: Int = 0, pwd: String, sessionFilterId: Int): List<HistoryRow> =
        filterExecMany(num = num, offset = offset, sessionFilterId = sessionFilterId) { builder ->
            builder.whereEquals("sd.path", pwd.sqlEscape())
            builder.whereLikeContains("c.command", filter.sqlEscape())
        }

    fun filterPwdSub(filter: String, num: Int, offset: Int = 0, pwd: String, sessionFilterId: Int): List<HistoryRow> =
        filterExecMany(num = num, offset = offset, sessionFilterId = sessionFilterId) { builder ->
            builder.whereLikePrefix("sd.path", pwd.sqlEscape())
            builder.whereLikeContains("c.command", filter.sqlEscape())
        }

    fun insertPreexec(now: String, dir: String, elevated: String, cmd: String, session: String): Int {
        val eDir = dir.sqlEscape()
        val eCmd = cmd.sqlEscape()
        return sql.execReturningId(
            """
INSERT OR IGNORE INTO directories (path)
VALUES ('$eDir');

INSERT OR IGNORE INTO commands (command)
VALUES ('$eCmd');

INSERT INTO history (
    start_epoch,
    start_dir_id,
    cmd_id,
    elevated_before,
    elevated_after,
    end_dir_id,
    exit_code,
    end_epoch,
    session
)
SELECT
    $now,
    d.id,
    c.id,
    $elevated,
    NULL,
    NULL,
    NULL,
    NULL,
    $session
FROM directories d
         JOIN commands c ON c.command = '$eCmd'
WHERE d.path = '$eDir'
RETURNING id;
        """.trimIndent()
        )
    }

    fun updatePrecmd(now: String, exitCode: Int, preexecId: Int, elevated: Int) {
        sql.rawExec(
            """
UPDATE history
SET
    end_epoch = $now,
    exit_code = $exitCode,
    elevated_after = $elevated,
    end_dir_id = start_dir_id
WHERE id = $preexecId;                
            """.trimIndent()
        ) { _, _ -> 0 }
    }
}
