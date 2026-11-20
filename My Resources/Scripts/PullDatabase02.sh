#!/bin/bash
# Pulls the newest Finance Tracker 5.0.0 backup from the phone into ~/Downloads/FinanceTracker.
#
# The app writes a single-file, checkpointed copy of its database on every backup: by hand (Extras › Back up data,
# into Documents/Finance Tracker/Backups) and by itself on the first launch of a day (Documents/Finance Tracker/
# Auto Backups, newest 7 kept). This script takes whichever of the two is newest, so usually no export is needed
# on the phone. Nothing in the app's private storage is read, so it works on any build and never sees a half-written
# WAL. The analysis scripts (FinanceManagerToGoogleSheetsInvestments03, StocksCapitalGainCalculator02,
# SpendAnalyzer02) read the newest "Finance Tracker Database - <date>.db" in the output folder; an auto backup is
# saved under that name with its time on the phone.
#
# Usage: PullDatabase02.sh            (the USB-connected phone)
#        DEVICE_SERIAL=<serial> PullDatabase02.sh

set -euo pipefail

ADB_PATH="/Users/nagabhushan/Library/Android/sdk/platform-tools/adb"
OUTPUT_DIRPATH="/Users/nagabhushan/Downloads/FinanceTracker"
MANUAL_DIR="/sdcard/Documents/Finance Tracker/Backups"
AUTO_DIR="/sdcard/Documents/Finance Tracker/Auto Backups"

if [ -n "${DEVICE_SERIAL:-}" ]; then
    ADB=("$ADB_PATH" -s "$DEVICE_SERIAL")
else
    ADB=("$ADB_PATH" -d)   # the one USB-connected device, never an emulator
fi

mkdir -p "$OUTPUT_DIRPATH"

# Newest file of each folder with its modification time (seconds): "<epoch>|<folder>|<name>"
newest_in() {
    local dir="$1"
    "${ADB[@]}" shell "cd \"$dir\" 2>/dev/null && ls -t *.db 2>/dev/null | head -1 | while read -r f; do echo \"\$(stat -c %Y \"\$f\")|$dir|\$f\"; done" | tr -d '\r'
}

candidates=$( { newest_in "$MANUAL_DIR"; newest_in "$AUTO_DIR"; } | sort -t'|' -k1,1nr )
if [ -z "$candidates" ]; then
    echo "No backup found on the phone in '$MANUAL_DIR' or '$AUTO_DIR'. Is it connected and unlocked?"
    exit 1
fi
newest=$(echo "$candidates" | head -1)
epoch=${newest%%|*}; rest=${newest#*|}; dir=${rest%%|*}; name=${rest#*|}

# An auto backup is named by its day only; give it the same name scheme as a manual backup, timed as on the phone
case "$name" in
    "Finance Tracker Auto Backup - "*)
        stamp=$(date -r "$epoch" "+%Y-%m-%d %H-%M-%S")
        out_name="Finance Tracker Database - ${stamp}.db" ;;
    *)  out_name="$name" ;;
esac
out_path="$OUTPUT_DIRPATH/$out_name"

echo "Newest backup on the phone: $dir/$name ($(date -r "$epoch" "+%d/%m/%Y %H:%M"))"
if [ -s "$out_path" ]; then
    echo "Already pulled: $out_path"
else
    "${ADB[@]}" pull "$dir/$name" "$out_path" > /dev/null
    echo "Pulled to: $out_path"
fi

# A quick look inside, so a wrong or damaged file is noticed now
if command -v sqlite3 > /dev/null; then
    sqlite3 "$out_path" "SELECT 'schema ' || (SELECT user_version FROM pragma_user_version) || ', ' || COUNT(*) || ' transactions, last dated ' || MAX(date) || ', integrity ' || (SELECT integrity_check FROM pragma_integrity_check) FROM transactions;"
fi
