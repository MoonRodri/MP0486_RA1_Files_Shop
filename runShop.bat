@echo off
REM Run Shop using classpath only (avoid module-path issues like duplicate jrt.fs/jdk.internal.jimage)
REM Usage: runShop.bat
java -cp "%~dp0target\classes" main.Shop %*
pause
