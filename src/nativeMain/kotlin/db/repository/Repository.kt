package db.repository

/**
 * A repository owns access to a single table/entity: reading, writing, and mapping raw
 * SQLite rows into domain models.
 */
sealed interface Repository<T> {
fun all(): List<T>
}