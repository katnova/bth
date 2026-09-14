package cli.command

import cli.OptSpec
import cli.ParsedArgs
import db.StorageLayer
import db.repository.BookmarkRepository

/**
 * Subcommand of [Bookmark]. Deletes a bookmark row from the db.
 */
data class BookmarkDelete(private val repository: () -> BookmarkRepository = { StorageLayer.nSqlite.bookmarks }) : Command {

    override val names: Set<String> = setOf("delete", "d")
    override val cmdHelp: String = "Delete a bookmark by id"

    val cmdId = OptSpec.Value(
        "cmd-id",
        isRequired = true,
        isNullable = false,
        convert = String::toInt,
        help = "The id of the bookmark to delete",
    )
    override val spec: List<OptSpec<*>>
        get() = listOf(cmdId)

    override fun execute(args: ParsedArgs) {
        repository().drop(args.value(cmdId))
    }
}
