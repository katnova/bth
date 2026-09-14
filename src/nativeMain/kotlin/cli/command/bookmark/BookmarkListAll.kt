package cli.command

import cli.OptSpec
import cli.ParsedArgs
import db.StorageLayer
import db.repository.BookmarkRepository

/**
 * Subcommand of [Bookmark]. Prints all bookmarks to stdout.
 */
data class BookmarkListAll(private val repository: () -> BookmarkRepository = { StorageLayer.nSqlite.bookmarks }) : Command {

    override val names: Set<String> = setOf("list", "l")
    override val cmdHelp: String = "List all bookmarks"
    override val spec: List<OptSpec<*>> = listOf()

    override fun execute(args: ParsedArgs) {
        repository().all().forEach {
            println(it.toString())
        }
    }
}
