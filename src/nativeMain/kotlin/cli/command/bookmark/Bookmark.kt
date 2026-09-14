package cli.command

import cli.ParsedArgs
import db.StorageLayer

data object Bookmark : SubcommandGroup(
    subcommands = listOf(BookmarkListAll(), BookmarkDelete(), BookmarkCreate())
) {
    override val names: Set<String> = setOf("bookmark")
    override val cmdHelp: String  = "Manage bookmarks"

    override fun execute(args: ParsedArgs) {
       StorageLayer.load()
       super.execute(args)
    }
}
