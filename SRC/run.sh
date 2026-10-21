#!/bin/bash
cd "$(dirname "$0")"
if [ -f "dist/NgocRongOnline.jar" ]; then
    cp -f "dist/NgocRongOnline.jar" "20.jar"
fi
java -server \
  -Xms256M -Xmx550M \
  -XX:MaxMetaspaceSize=96M \
  -Xss256k \
  -XX:+UseSerialGC \
  -Dfile.encoding=UTF-8 \
  -jar 20.jar