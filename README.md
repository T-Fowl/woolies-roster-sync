woolies-roster-sync
===================

[comment]: <> (TODO: Add badges [build, licence, version])

[comment]: <> (TODO: Add a short description)

# Usage

```shell
> ./woolies-roster-sync --help 
Usage: woolies-roster-sync [OPTIONS]

Options:
  --calendar-id TEXT               ID of the destination google calendar
                                   (required)
  --secrets PATH                   Google calendar api secrets file (default:
                                   client-secrets.json)
  --cookies FILE                   Cookies file in netscape format. Only
                                   needed on first run or when stored tokens
                                   have expired
  --token TEXT                     Workjam jwt
  --sync-period-start OFFSET_DATE_TIME
                                   Date to start syncing shifts, in the
                                   ISO_OFFSET_DATE_TIME format (Example:
                                   2007-12-03T10:15:30+01:00) (default: now)
  --sync-period-end OFFSET_DATE_TIME
                                   Date to finish syncing shifts, in the
                                   ISO_OFFSET_DATE_TIME format (Example:
                                   2007-12-03T10:15:30+01:00) (default: 15
                                   days from now)
  -h, --help                       Show this message and exit

```

[comment]: <> (TODO: Add a simple explanation for usage)

# Download

[comment]: <> (TODO: Add a note on downloading)

# Licence

[comment]: <> (TODO: Decide on a licence)