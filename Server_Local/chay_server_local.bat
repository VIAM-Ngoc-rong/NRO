@ECHO OFF
chcp 65001 >nul
title Ngoc Rong VIAM - SERVER LOCAL 127.0.0.1:14445
cd /d "%~dp0"

echo ============================================================
echo   KHOI DONG SERVER LOCAL - NGOC RONG VIAM 127.0.0.1:14445
echo ============================================================

tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe" >NUL
if errorlevel 1 if exist "C:\xampp\mysql\bin\mysqld.exe" start "" /B "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
if errorlevel 1 timeout /t 3 /nobreak >nul

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":14445" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1

if exist "Config.properties" copy /Y "Config.properties" "..\SRC\Config.properties" >nul 2>&1
if exist "C:\Users\Gigabyte\.gemini\antigravity\brain\aedbcf3d-0e19-4529-80f5-97b8f5824fbd\scratch\NgocRongOnline_new.jar" copy /Y "C:\Users\Gigabyte\.gemini\antigravity\brain\aedbcf3d-0e19-4529-80f5-97b8f5824fbd\scratch\NgocRongOnline_new.jar" "..\SRC\dist\NgocRongOnline.jar" >nul 2>&1
if exist "..\SRC\dist\NgocRongOnline.jar" copy /Y "..\SRC\dist\NgocRongOnline.jar" "..\SRC\20.jar" >nul 2>&1
if exist "..\SRC\20.jar" copy /Y "..\SRC\20.jar" "20.jar" >nul 2>&1

echo Dang khoi chay Server Local 127.0.0.1:14445...
echo ------------------------------------------------------------
cd /d "%~dp0..\SRC"
"C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -server -Xms1024M -Xmx2048M -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Dfile.encoding=UTF-8 -jar 20.jar
PAUSE
