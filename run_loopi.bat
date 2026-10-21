@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "APK_PATH=%PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk"

if defined LOOPI_ANDROID_SDK set "SDK_DIR=%LOOPI_ANDROID_SDK%"
if not defined SDK_DIR if defined USERPROFILE if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" set "SDK_DIR=%USERPROFILE%\AppData\Local\Android\Sdk"
if not defined SDK_DIR if defined LOCALAPPDATA if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "SDK_DIR=%LOCALAPPDATA%\Android\Sdk"
if not defined SDK_DIR if defined ANDROID_SDK_ROOT set "SDK_DIR=%ANDROID_SDK_ROOT%"
if not defined SDK_DIR if defined ANDROID_HOME set "SDK_DIR=%ANDROID_HOME%"

if not defined SDK_DIR (
  echo Android SDK was not found. Set ANDROID_SDK_ROOT or ANDROID_HOME first.
  exit /b 1
)
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
if not exist "%ADB%" (
  echo Android Debug Bridge was not found at: %ADB%
  exit /b 1
)
set "ANDROID_HOME=%SDK_DIR%"
set "ANDROID_SDK_ROOT=%SDK_DIR%"

set "DEVICE="
for /f "tokens=1,2" %%A in ('"%ADB%" devices') do (
  if "%%B"=="device" if not "%%A"=="List" (
    set "DEVICE=%%A"
    goto :device_found
  )
)
:device_found
if not defined DEVICE (
  echo No Android device or emulator is running.
  echo Start one in Android Studio Device Manager, then run this file again.
  exit /b 1
)

echo Building Loopi (five main flows plus Study Packs Flow 6)...
pushd "%PROJECT_DIR%"
call gradlew.bat :app:assembleDebug
if errorlevel 1 (
  popd
  echo Build failed. Review the Gradle messages above.
  exit /b 1
)
popd

echo Installing on %DEVICE%...
"%ADB%" -s "%DEVICE%" install -r "%APK_PATH%"
if errorlevel 1 exit /b 1

echo Opening Loopi...
"%ADB%" -s "%DEVICE%" shell am start -n com.example.myapplication/.MainActivity
if errorlevel 1 exit /b 1

echo Loopi is running.
endlocal
