#!/bin/bash

# Define paths
ADB_PATH="/Users/nagabhushan/Library/Android/sdk/platform-tools/adb"
ANDROID_PATH="/sdcard/FinanceManager/TransferPhoneToMacbook/"
LOCAL_PATH="$HOME/Downloads/FinanceManager/"

# Ensure ADB is connected
"$ADB_PATH" devices | grep -w "device" > /dev/null
if [ $? -ne 0 ]; then
    echo "No ADB device found. Please connect your phone and enable USB Debugging."
    exit 1
fi

# Create local directory if it doesn't exist
mkdir -p "$LOCAL_PATH"

# Copy files
"$ADB_PATH" pull "$ANDROID_PATH" "$LOCAL_PATH"

echo "Transfer from Android to MacBook completed."
