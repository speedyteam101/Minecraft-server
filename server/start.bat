@echo off
rem Starts the Demon Slayer Paper server (Minecraft Java 1.20.1) on Windows.
cd /d "%~dp0"

if exist paper.jar goto havepaper
echo paper.jar was not found.
echo Download Paper 1.20.1 from https://papermc.io/downloads/all and save it in this folder as paper.jar
pause
exit /b 1

:havepaper
if not exist plugins mkdir plugins
if not exist "..\plugin\target\DemonSlayer-*.jar" goto eula
del /q "plugins\DemonSlayer-*.jar" 2>nul
copy /y "..\plugin\target\DemonSlayer-*.jar" plugins\ >nul

:eula
findstr /b "eula=true" eula.txt >nul 2>&1
if not errorlevel 1 goto run
echo To run a Minecraft server you must agree to the Minecraft EULA: https://aka.ms/MinecraftEULA
set "answer="
set /p "answer=Do you agree? (yes/no) "
if /i not "%answer%"=="yes" goto noeula
echo eula=true> eula.txt
goto run

:noeula
echo You need to accept the EULA to run the server.
pause
exit /b 1

:run
java -Xms2G -Xmx2G -jar paper.jar --nogui
pause
