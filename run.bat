@echo off
REM Windows: double-click or run  run.bat   (needs JDK 17+)
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt && java -cp out com.autosorter.Main %1
