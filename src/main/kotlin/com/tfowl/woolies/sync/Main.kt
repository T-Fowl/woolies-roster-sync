package com.tfowl.woolies.sync

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.tfowl.woolies.sync.commands.Authorise
import com.tfowl.woolies.sync.commands.Sync

class WooliesRosterSync : NoOpCliktCommand() {
    init {
        context {
            helpFormatter = CliktHelpFormatter(
                showDefaultValues = true,
                showRequiredTag = true,
            )
        }
    }
}

fun main(vararg args: String) = WooliesRosterSync().subcommands(
    Sync(), Authorise()
).main(args)