@echo off
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
  echo Khong tim thay javac. Hay cai JDK 17 tro len.
  pause
  exit /b 1
)

if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo Build that bai.
  pause
  exit /b 1
)

echo Main-Class: MarkdownSplitter> manifest.txt
jar cfm MarkdownFileSplitter.jar manifest.txt -C out .
del manifest.txt
echo Build thanh cong: MarkdownFileSplitter.jar
pause
