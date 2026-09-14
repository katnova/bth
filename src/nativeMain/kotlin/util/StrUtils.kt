package util

fun String.sqlEscape(): String = this.replace("'", "''")