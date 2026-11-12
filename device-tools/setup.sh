#!/system/bin/sh
set -eu
# Run as root. This changes only this module and the two legacy edge adapters.
apk=$(pm path dev.codex.realmefullscreen | head -n 1 | cut -d: -f2-)
test -n "$apk"
export CLASSPATH=/data/local/tmp/fullscreen-setup.dex
app_process /system/bin LsposedSetup setup "$apk"
