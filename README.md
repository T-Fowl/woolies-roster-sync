woolies-roster-sync
===================

Whilst I would like to create proper released and documentation - as I only use
this project personally there is currently little incentive

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

TODO

[comment]: <> (TODO: Add a note on downloading)

# Licence

```
MIT License

Copyright (c) 2022 Thomas Fowler

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```