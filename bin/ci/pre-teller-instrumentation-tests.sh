#!/bin/bash

mkdir -p teller/build/outputs/apk/debug/

cp static/app-debug-apk-testing.apk teller/build/outputs/apk/debug/app-debug.apk

# Bitrise hack to change modified date of the file to the current time. 
touch teller/build/outputs/apk/debug/app-debug.apk