@ECHO OFF
chcp 65001 >nul
title Ngoc Rong VIAM - SERVER LOCAL (127.0.0.1:14445)
cd /d "%~dp0"

echo ============================================================
echo   KHOI DONG SERVER LOCAL - NGOC RONG VIAM (127.0.0.1:14445)
echo ============================================================

:: 1. Kiem tra va tu dong bat MySQL (XAMPP) neu chua chay
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%" NEQ "0" (
    if exist "C:\xampp\mysql\bin\mysqld.exe" (
        echo [1/3] MySQL XAMPP chua chay -> Dang tu dong khoi dong MySQL...
        start "" /B "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
        timeout /t 3 /nobreak >nul
    ) else (
        echo [CANH BAO] Khong tim thay MySQL XAMPP dang chay! Hay bat MySQL truoc.
    )
) else (
    echo [1/3] MySQL XAMPP dang hoat dong tot.
)

:: 2. Tu dong lay ban build 20.jar moi nhat tu thu muc SRC
if exist "..\SRC\20.jar" (
    echo [2/3] Dang dong bo 20.jar moi nhat tu E:\NRO\SRC\20.jar...
    copy /Y "..\SRC\20.jar" "20.jar" >nul
) else if exist "..\SRC\dist\NgocRongOnline.jar" (
    echo [2/3] Dang dong bo NgocRongOnline.jar tu E:\NRO\SRC\dist...
    copy /Y "..\SRC\dist\NgocRongOnline.jar" "20.jar" >nul
)

:: 3. Khoi chay Server Local
echo [3/3] Dang khoi chay Server Local...
echo ------------------------------------------------------------
java -server -Xms1024M -Xmx2048M -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Dfile.encoding=UTF-8 -jar 20.jar
PAUSE
