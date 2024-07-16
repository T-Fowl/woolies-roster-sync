package com.tfowl.woolies.sync

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.tfowl.woolies.sync.commands.Authorise
import com.tfowl.woolies.sync.commands.Sync

fun CliktCommand.googleCalendarOption() = option(
    "--calendar",
    envvar = "GOOGLE_CALENDAR_ID",
    help = "ID of the destination google calendar"
)

fun CliktCommand.googleClientSecretsOption() = mutuallyExclusiveOptions(
    option("--google-client-secrets").file().convert { it.reader() },
    option("--google-secrets", envvar = "GOOGLE_CLIENT_SECRETS").convert { it.reader() }
).single()


class WooliesRosterCommand : NoOpCliktCommand(name = "woolies-roster") {
    init {
        context {
            helpFormatter = {
                MordantHelpFormatter(
                    context = it,
                    showDefaultValues = true,
                    showRequiredTag = true,
                )
            }
        }
    }
}

fun main(vararg args: String) = WooliesRosterCommand().subcommands(
    Sync(), Authorise()
).main(args)