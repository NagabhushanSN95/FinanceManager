#!/bin/bash

# Variables (customize these as needed)
ADB_PATH="/Users/nagabhushan/Library/Android/sdk/platform-tools/adb"
PACKAGE_NAME="com.chaturvedi.financemanager"  # Replace with your app's package name
DB_NAME="expenditureManager"                 # Replace with your database name
ANDROID_STUDIO_VERSION="2024.2"              # Replace with your Android Studio version
DEVICE_NAME="Google Pixel 8a"               # Replace with your device name
#OUTPUT_PATH="/Users/nagabhushan/Library/Caches/Google/AndroidStudio${ANDROID_STUDIO_VERSION}/device-explorer/${DEVICE_NAME}/_/data/data/${PACKAGE_NAME}/databases/${DB_NAME}"
OUTPUT_PATH="/Users/nagabhushan/Downloads/FinanceManager/${DB_NAME}"
OUTPUT_DIRPATH=$(dirname "$OUTPUT_PATH")

# Ensure OUTPUT_PATH exists
mkdir -p "$OUTPUT_DIRPATH"

# Pull the database directly using adb exec-out
"$ADB_PATH" shell "run-as $PACKAGE_NAME cat /data/data/$PACKAGE_NAME/databases/$DB_NAME" > "$OUTPUT_PATH"

# Verify if the database file was successfully pulled
if [ -s "$OUTPUT_PATH" ]; then
    echo "Database file pulled successfully to: $OUTPUT_PATH"
else
    echo "Failed to pull the database file. Check permissions and file path."
fi
