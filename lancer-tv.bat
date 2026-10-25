@echo off
rem Lance l'emulateur Android TV puis ouvre l'appli des que la TV a demarre
set SDK=%LOCALAPPDATA%\Android\Sdk
set ADB=%SDK%\platform-tools\adb.exe

start "" "%SDK%\emulator\emulator.exe" -avd Television_4K_API_36 -no-snapshot-save

echo Demarrage de la TV...
"%ADB%" wait-for-device
:boot
for /f "delims=" %%b in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set BOOTED=%%b
if not "%BOOTED%"=="1" (
    timeout /t 3 /nobreak >nul
    goto boot
)

"%ADB%" install -r "%~dp0app\build\outputs\apk\debug\app-debug.apk" >nul
"%ADB%" shell am start -n com.djamal.ch.netflix/.MainActivity >nul
echo Appli lancee.
