package cli.command

import cli.OptSpec
import cli.ParsedArgs
import db.StorageLayer
import db.repository.BookmarkRepository

/**
 * Subcommand of [Bookmark]. Inserts a new bookmark record into the db.
 */
data class BookmarkCreate(private val repository: () -> BookmarkRepository = { StorageLayer.nSqlite.bookmarks }) : Command {

    override val names: Set<String> = setOf("create", "c")
    override val cmdHelp: String = "Create a bookmark and get it's id"

    val cmdId = OptSpec.Value(
        "cmd-id",
        isRequired = true,
        isNullable = false,
        convert = String::toInt,
        help = "The id of the command to bookmark",
    )

    val groupId = OptSpec.Value<Int?>(
        "group-id",
        isRequired = false,
        isNullable = true,
        convert = String::toInt,
        help = "The id of the group the bookmark should belong to",
    )

    override val spec: List<OptSpec<*>> = listOf(cmdId, groupId)

    override fun execute(args: ParsedArgs) {
        val id = repository().insert(
            cmdId = args.value(cmdId),
            groupId = args.value(groupId)
        )
        println(id)
    }
}
