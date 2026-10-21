@ECHO OFF
if exist "dist\NgocRongOnline.jar" copy /Y "dist\NgocRongOnline.jar" "20.jar" >nul 2>&1
java -server -Dfile.encoding=UTF-8  -jar 20.jar
PAUSE

PAUSE