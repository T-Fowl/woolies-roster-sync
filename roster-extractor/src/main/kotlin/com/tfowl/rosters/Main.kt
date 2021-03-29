package com.tfowl.rosters

import java.io.File


fun main(vararg args: String) {
    require(args.isNotEmpty()) { "Usage: [exec] roster-file" }

    val rosters = RosterReader().read(File(args[0]))
}
