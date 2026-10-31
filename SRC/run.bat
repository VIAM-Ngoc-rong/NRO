@ECHO OFF
chcp 65001 >nul
title Ngoc Rong VIAM Server - Local 127.0.0.1:14445
cd /d "%~dp0"

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":14445" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1

tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe" >NUL
if errorlevel 1 if exist "C:\xampp\mysql\bin\mysqld.exe" start "" /B "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
if errorlevel 1 timeout /t 3 /nobreak >nul

if exist "dist\NgocRongOnline.jar" copy /Y "dist\NgocRongOnline.jar" "20.jar" >nul 2>&1
echo Dang khoi chay Server tai E:\NRO\SRC - 127.0.0.1:14445...
"C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -server -Xms1024M -Xmx2048M -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Dfile.encoding=UTF-8 -jar 20.jar
PAUSE
