package cli

import cli.command.Command
import exception.UnknownArgumentException
import util.logWarn

object CommandDispatcher {
    fun execute(args: Array<String>) {
        val name = args.firstOrNull() ?: throw UnknownArgumentException(
            "No command given. Run 'bth help' for usage."
        )

        val command = Command.entries.firstOrNull { name in it.names }
        if (command == null) {
            logWarn("Unknown command `$name`: ${args.joinToString()}", true)
            return
        }

        val rest = args.drop(1)
        if (rest.contains("--help")) {
            val target = findHelpTarget(command, rest)
            println(target.usage())
            return
        }

        val tokens = Tokenizer().tokenize(rest.toTypedArray())
        when (val result = Binder.bind(tokens, command.spec)) {
            is ParseResult.Success -> command.execute(result.value)
            is ParseResult.Failure -> throw result.exception
        }
    }

    fun findHelpTarget(root: Command, rest: List<String>): Command {
        var current = root
        var remaining = rest
        while (remaining.isNotEmpty()) {
            val next = remaining.first()
            if (next == "--help") break
            val sub = current.subcommands.firstOrNull { next in it.names } ?: break
            current = sub
            remaining = remaining.drop(1)
        }
        return current
    }
}
