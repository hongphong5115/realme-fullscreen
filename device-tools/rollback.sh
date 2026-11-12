#!/system/bin/sh
set -eu
# Run as root with the matching maintenance DEX present. Reboot after this command.
export CLASSPATH=/data/local/tmp/fullscreen-setup.dex
app_process /system/bin LsposedSetup rollback
