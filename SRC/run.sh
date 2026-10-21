#!/bin/bash
cd "$(dirname "$0")"
if [ -f "dist/NgocRongOnline.jar" ]; then
    cp -f "dist/NgocRongOnline.jar" "20.jar"
fi
exec nice -n 15 ionice -c 2 -n 7 java -server \
  -Xms256M -Xmx480M \
  -XX:MaxMetaspaceSize=80M \
  -Xss256k \
  -XX:+UseSerialGC \
  -XX:ActiveProcessorCount=1 \
  -Dfile.encoding=UTF-8 \
  -cp "lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
  nro.models.server.ServerManager