package cinterop

/**
 *
 * Heavily referenced Ksqlite here.
 * https://github.com/Kotlin/kotlinconf-spinner/blob/b1e66920ac7216d3dbc4b6b4625fb77efe23cff8/sql/src/hostMain/kotlin/ksqlite/KSqlite.kt
 *
 */

import cnames.structs.sqlite3
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import sqlite3.sqlite3_close
import sqlite3.sqlite3_errmsg
import sqlite3.sqlite3_exec
import sqlite3.sqlite3_free
import sqlite3.sqlite3_open

class NativeSqlite3Error(msg: String): Error(msg)

@OptIn(ExperimentalForeignApi::class)
typealias NativeSqliteConn = CPointer<sqlite3>?

//todo: More elegant null-handling, hate this
@OptIn(ExperimentalForeignApi::class)
private fun fromCArray(ptr: CPointer<CPointerVar<ByteVar>>?, count: Int) =
    Array(count) { index ->
        val element = (ptr + index)?.pointed?.value
        element?.toKString() ?: ""
    }


@OptIn(ExperimentalForeignApi::class)
class NativeSqlite3 {
    val sqlitePath: String
    var conn: NativeSqliteConn

    constructor(sqlitePath: String) {
        this.sqlitePath = sqlitePath
        memScoped {
            val sqlitePointer = alloc<CPointerVar<sqlite3>>()
            if (sqlite3_open(sqlitePath, sqlitePointer.ptr) != 0) {
                throw NativeSqlite3Error("Cannot open sqlitedb: ${sqlite3_errmsg(sqlitePointer.value)}")
            }
            conn = sqlitePointer.value
        }
    }


    /**
     * If no callback, discard response.
     */
    fun rawExec(command: String, callback: ((Array<String>, Array<String>)-> Int)? = null) {
        memScoped {
            val error = this.alloc<CPointerVar<ByteVar>>()
            val callbackStable = if (callback != null) StableRef.create(callback) else null
            try {
                if (sqlite3_exec(conn, command, if (callback != null)
                        staticCFunction {
                                ptr, count, data, columns ->
                            val callbackFunction = ptr!!.asStableRef<(Array<String>, Array<String>) -> Int>().get()
                            val columnsArray = fromCArray(columns!!, count)
                            val dataArray = fromCArray(data, count) //todo: Really want non-reified generic conversion, might be more headache than it's worth
                            callbackFunction(columnsArray, dataArray)
                        } else null,
                        callbackStable?.asCPointer(),
                        error.ptr
                ) != 0)
                throw NativeSqlite3Error("DB error: ${error.value!!.toKString()}: $command")
            } finally {
                callbackStable?.dispose()
                sqlite3_free(error.value)
            }
        }
    }

    /**
     * Presumes `RETURNING <COL>` of type Int.
     */
    fun execReturningId(command: String): Int {
        var returnedId: Int = -1
        rawExec(command) { _, data ->
            if (data.isNotEmpty()) {
                returnedId = data[0].toInt()
            }
            0
        }
        return returnedId
    }

    /**
     * Collects many returned items from a query at once to prevent the need to use the raw callback
     */
    fun execMany(command: String): MutableList<Array<String>> {
        val data: MutableList<Array<String>> = mutableListOf()
        rawExec(command) { _, dat ->
            data.add(dat)
            0
        }
        return data
    }

    fun close() {
        if(conn != null) {
            sqlite3_close(conn)
            conn = null
        }
    }
}