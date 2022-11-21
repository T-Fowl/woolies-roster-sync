rootProject.name = "woolies-roster-sync"

include("workjam")

include(":gcal-sync-kt")
project(":gcal-sync-kt").projectDir = rootProject.projectDir.resolve("gcal-sync-kt")