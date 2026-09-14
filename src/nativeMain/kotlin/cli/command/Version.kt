package cli.command

import buildinfo.BuildInfo
import cli.OptSpec
import cli.ParsedArgs

data object Version : Command {
    override val names = setOf("version", "--version")
    override val cmdHelp = "Outputs the BTH version, copyright, and license information."
    override val spec = emptyList<OptSpec<*>>()

    override fun execute(args: ParsedArgs) {
        println(
            """
            bth ${BuildInfo.VERSION}
            Copyright (C) 2026 akat@akat.xyz
            BSD 3-Clause "New" or "Revised" License <https://spdx.org/licenses/BSD-3-Clause.html>
        """.trimIndent()
        )
    }
}
