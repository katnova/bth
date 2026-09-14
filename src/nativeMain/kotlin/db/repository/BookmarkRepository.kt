package db.repository

import cinterop.NativeSqlite3

data class BookmarkRow(
    val id: Int,
    val cmdId: Int,
    val groupId: Int?,
) {
    override fun toString(): String {
        return "id=$id, cmdId=$cmdId, groupId=$groupId"
    }

}

class BookmarkRepository(private val sql: NativeSqlite3): Repository<BookmarkRow> {
    override fun all(): List<BookmarkRow> {
        return sql.execMany("SELECT id, cmd_id, \"group\" FROM bookmarks;").map {
            BookmarkRow(it[0].toInt(), it[1].toInt(), it[2].toIntOrNull())
        }
    }

    fun drop(id: Int) {
        sql.rawExec("DELETE FROM bookmarks WHERE id = $id;")
    }

    fun insert(cmdId: Int, groupId: Int?): Int {
        val groupVal = groupId?.toString() ?: "NULL"
        return sql.execReturningId("INSERT INTO bookmarks (cmd_id, \"group\") VALUES ($cmdId, $groupVal) RETURNING id;")
    }
}