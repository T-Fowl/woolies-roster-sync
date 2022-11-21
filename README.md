woolies-roster-sync
===================

[comment]: <> (TODO: Add badges [build, licence, version])

[comment]: <> (TODO: Add a short description)

# Usage

```shell
Usage: woolies-roster [OPTIONS] COMMAND [ARGS]...

Options:
  -h, --help  Show this message and exit

Commands:
  sync       Sync your roster from workjam to your calendar
```

```shell
Usage: woolies-roster sync [OPTIONS]

  Sync your roster from workjam to your calendar

Options:
  --calendar-id TEXT      ID of the destination google calendar (required)
  --secrets PATH          Google calendar api secrets file (default:
                          client-secrets.json)
  --cookies FILE          Cookies file in netscape format
  --token TEXT            Workjam jwt
  --sync-from LOCAL_DATE  Local date to start syncing shifts, will sync from
                          midnight at the start of the day (Example:
                          2007-12-03) (default: today)
  --sync-to LOCAL_DATE    Local date to finish syncing shifts, will sync until
                          midnight at the end of the day (Example: 2007-12-03)
                          (default: a month from sync-from)
  -h, --help              Show this message and exit

```

[comment]: <> (TODO: Add a simple explanation for usage)

# Download

[comment]: <> (TODO: Add a note on downloading)

# Licence

[comment]: <> (TODO: Decide on a licence)