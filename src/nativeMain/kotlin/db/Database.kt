package db

import db.repository.BookmarkRepository
import db.repository.CommandRepository
import db.repository.DirectoryRepository
import db.repository.HistoryRepository
import db.repository.SessionRepository

/**
 * Facade exposing the per-entity repositories, plus cross-entity operations (e.g. [purge],
 * [tableCounts]) that don't cleanly belong to a single repository.
 */
interface Database : AutoCloseable {
    val commands: CommandRepository
    val directories: DirectoryRepository
    val sessions: SessionRepository
    val history: HistoryRepository
    val bookmarks: BookmarkRepository

    fun purge()
    fun tableCounts(): Triple<Long, Long, Long>
}
