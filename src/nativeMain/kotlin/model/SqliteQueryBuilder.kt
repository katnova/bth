package model

import util.sqlEscape

class SqliteQueryBuilder(private val baseSelectFrom: String) {
    private val where = mutableListOf<String>()
    private var orderBy: String? = null
    private var limit: Int? = null
    private var offset: Int? = null

    fun whereRaw(clause: String) = apply { where += clause }
    fun whereEquals(column: String, value: String) = apply {
        where += "$column = '${value.sqlEscape()}'"
    }
    fun whereLikeContains(column: String, value: String) = apply {
        where += "$column LIKE '%${value.sqlEscape()}%'"
    }
    fun whereLikePrefix(column: String, value: String) = apply {
        where += "$column LIKE '${value.sqlEscape()}%'"
    }

    fun orderBy(clause: String) = apply { orderBy = clause }
    fun limit(value: Int) = apply { limit = value.coerceAtLeast(0) }
    fun offset(value: Int) = apply { offset = value.coerceAtLeast(0) }

    fun build(): String {
        return buildString {
            append(baseSelectFrom)
            if (where.isNotEmpty()) {
                append("\nWHERE ")
                append(where.joinToString("\n  AND "))
            }
            orderBy?.let { append("\nORDER BY $it") }
            limit?.let { append("\nLIMIT $it") }
            offset?.let { append("\nOFFSET $it") }
            append(";")
        }
    }
}