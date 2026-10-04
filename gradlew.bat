@echo off
setlocal

where gradle >NUL 2>NUL
if errorlevel 1 (
  echo Gradle is not installed or not on PATH. Install Gradle or use a project wrapper.
  exit /b 1
)

gradle %*
