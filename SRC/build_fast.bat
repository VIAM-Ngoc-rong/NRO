@ECHO OFF
chcp 65001 >nul
cd /d "%~dp0"
set "JDK_BIN=C:\Program Files\Java\jdk-21.0.12.1\bin"

echo [1/3] Dang bien dich nhanh cac file su kien va thong bao...
"%JDK_BIN%\javac.exe" -proc:none -encoding UTF-8 -cp "lib\*;build\classes" -d "build\classes" ^
 src\nro\models\server\ServerNotify.java ^
 src\nro\models\services_func\UseItem.java ^
 src\nro\models\services\ItemService.java ^
 src\nro\models\services\RewardService.java ^
 src\nro\models\services\shenron\Shenron_Service.java ^
 src\nro\models\services\shenron\Shenron_Event.java ^
 src\nro\models\server\Manager.java ^
 src\nro\models\event\VuaHung.java ^
 src\nro\models\event\NoiBanh.java ^
 src\nro\models\event\XeNuocMia.java ^
 src\nro\models\boss\BossesData.java ^
 src\nro\models\boss\event_trung_thu\NguyetThan.java ^
 src\nro\models\boss\event_trung_thu\NhatThan.java ^
 src\nro\models\boss\event_trung_thu\KhiDot.java ^
 src\nro\models\boss\event\Halloween\BiMa.java ^
 src\nro\models\boss\event\Halloween\MaTroi.java ^
 src\nro\models\boss\event\Halloween\Doi.java ^
 src\nro\models\boss\event_hung_vuong\SonTinh.java ^
 src\nro\models\boss\event_hung_vuong\ThuyTinh.java

if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Bien dich that bai!
    pause
    exit /b 1
)

echo [2/3] Dang cap nhat dist\NgocRongOnline.jar...
"%JDK_BIN%\jar.exe" uf "dist\NgocRongOnline.jar" -C "build\classes" nro

echo [3/3] Dang dong bo sang 20.jar...
copy /Y "dist\NgocRongOnline.jar" "20.jar" >nul 2>&1

echo === HOAN TAT BUILD NHANH TRONG VAI GIAY! ===
pause
