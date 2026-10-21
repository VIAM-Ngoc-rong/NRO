#!/bin/bash
cd "$(dirname "$0")"
if [ -f "dist/NgocRongOnline.jar" ]; then
    cp -f "dist/NgocRongOnline.jar" "20.jar"
fi
java -server \
  -Xms512M -Xmx850M \
  -XX:MaxMetaspaceSize=128M \
  -Xss256k \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:+UseStringDeduplication \
  -Dfile.encoding=UTF-8 \
  -jar 20.jar