@ECHO OFF
title Ngoc Rong VIAM Server
if exist "dist\NgocRongOnline.jar" copy /Y "dist\NgocRongOnline.jar" "20.jar" >nul 2>&1
java -server -Xms1024M -Xmx2048M -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Dfile.encoding=UTF-8 -jar 20.jar
PAUSE
