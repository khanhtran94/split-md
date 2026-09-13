@echo off
setlocal
cd /d "%~dp0"

if not exist MarkdownFileSplitter.jar call build.bat
if not exist MarkdownFileSplitter.jar exit /b 1

javaw -jar MarkdownFileSplitter.jar
if errorlevel 1 (
  echo Khong the chay app. Hay kiem tra Java 17 tro len.
  pause
)
