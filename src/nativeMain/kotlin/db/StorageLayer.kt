package db

object StorageLayer {
    lateinit var nSqlite: Database

    fun load() {
        nSqlite = NativeSqliteService()
    }
}