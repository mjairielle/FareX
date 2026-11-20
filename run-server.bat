@echo off
rem Run from the FareX root so .env and data\ are found. No Maven needed.
rem shortcut: compiles shared and server together each run, fine for a demo.
if exist build-server rmdir /s /q build-server
mkdir build-server
dir /s /b farex-shared\src\main\java\*.java > build-server\sources.txt
dir /s /b farex-server\src\main\java\*.java >> build-server\sources.txt
javac -d build-server @build-server\sources.txt || exit /b 1
java -cp build-server farex.server.Main
