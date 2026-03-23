@echo off
setlocal
cd /d %~dp0

echo ========================================
echo  DokodemoTV: Pull and Build Tool
echo ========================================

echo [1/2] Pulling latest changes from GitHub...
git pull
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Git pull failed. Please check your connection or merge conflicts.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Building Debug APK...
call gradlew.bat assembleDebug --no-daemon
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed. Please check the logs above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================
echo  [SUCCESS] Build Completed!
echo ========================================
echo APK Path: app\build\outputs\apk\debug\app-debug.apk
echo.
pause
