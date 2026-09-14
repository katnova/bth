package cli

import cli.Token.*

class Tokenizer {
    /**
     * Does not support args with spaces in their values.
     */
    fun tokenize(args: Array<String>): List<Token> {
        var skip = false
        val opts = mutableListOf<Token>()
        var foundEndOfOpts = false
        for ((index, value) in args.withIndex()) {
            if (skip) {
                skip = false
                if (value != "--" && !value.startsWith("-")) {
                    opts.add(LongOpt(args[index - 1].drop(2), value))
                    continue
                } else {
                    opts.add(LongOpt(args[index - 1].drop(2), ""))
                }
            }

            if (value == "--") {
                opts.add(EndOfOpts("--"))
                foundEndOfOpts = true
                continue
            }

            if (!foundEndOfOpts && value.startsWith("--") && value.length > 3) {
                // handle `--k v` and `--k=v`
                if(value.contains("=")) {
                    val tokens = value.drop(2).split("=")
                    opts.add(LongOpt(tokens[0], tokens[1]))
                } else {
                    skip = true
                }
            } else if(!foundEndOfOpts && value.startsWith("-") && value.length == 2) {
                //handle flag `-k`
                opts.add(ShortOpt(value.drop(1)[0]))
            } else if (foundEndOfOpts) {
                if(opts.last() is Positional) {
                    opts[opts.lastIndex] = Positional("${(opts.last() as Positional).value} $value")
                } else {
                    opts.add(Positional(value))
                }
            } else {
                opts.add(Unknown(value))
            }
        }

        return opts
    }
}