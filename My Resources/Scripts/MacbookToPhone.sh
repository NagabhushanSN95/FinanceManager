#!/bin/bash

# Define paths
ADB_PATH="/Users/nagabhushan/Library/Android/sdk/platform-tools/adb"
LOCAL_PATH="$HOME/Downloads/FinanceManager/TransferMacbookToPhone/"
ANDROID_PATH="/sdcard/FinanceManager/"

# Ensure ADB is connected
"$ADB_PATH" devices | grep -w "device" > /dev/null
if [ $? -ne 0 ]; then
    echo "No ADB device found. Please connect your phone and enable USB Debugging."
    exit 1
fi

# Copy files
"$ADB_PATH" shell "mkdir -p $ANDROID_PATH"
"$ADB_PATH" push "$LOCAL_PATH" "$ANDROID_PATH"

echo "Transfer from MacBook to Android completed."
