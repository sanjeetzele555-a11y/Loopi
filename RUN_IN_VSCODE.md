# Run Loopi from the VS Code terminal

These steps build the Android app, install it on an emulator or connected Android phone, and open it. The project is at `E:\Loopi_Final` and uses the Gradle wrapper already included in the folder.

## 1. Install the tools

- Install Visual Studio Code.
- Install Android Studio and, in SDK Manager, install Android SDK Platform 37 and Android SDK Build-Tools 36. Accept the SDK licenses.
- Install the Java version requested by this project (JDK 25), or allow Gradle to download its configured toolchain on the first build.
- In Android Studio, open Device Manager and create/start an Android Virtual Device (AVD). A physical Android phone with USB debugging enabled also works.

The first Gradle build needs internet access to download Gradle and project dependencies.

## 2. Open the project in VS Code

Open the `E:\Loopi_Final` folder in VS Code. Choose **Terminal → New Terminal** and use PowerShell. Run:

```powershell
Set-Location E:\Loopi_Final
```

## 3. Start and check your device

Start an emulator in Android Studio Device Manager, or connect/unlock your phone and allow its USB debugging prompt. Then check that Android sees it:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
& $adb devices
```

The device should appear with the status `device`. If your Android SDK is in a different folder, set `$sdk` to that folder instead.

### If Android Studio shows no devices

The test emulator used for the earlier run was a **Pixel 7, Android 37.2 (Google APIs Play Store / x86_64)** AVD named `LoopiQA`. Its files are stored at `E:\Loopi\qa_avds\LoopiQA.avd`, outside this final project. It is not currently running. Since it uses a custom AVD folder, it may not show in Android Studio's regular Device Manager list.

**Recommended: create an emulator visible in Android Studio**

1. Open Android Studio and choose **Tools → Device Manager** (from the welcome screen, choose **More Actions → Virtual Device Manager**).
2. Choose **Create Device**, select **Pixel 7**, then choose **Next**.
3. Select or download an **API 37 Google APIs x86_64** system image, then choose **Next → Finish**.
4. In Device Manager, press the **▶** button beside the new virtual device and wait for its home screen to finish loading.
5. In the VS Code terminal, check it is connected:

   ```powershell
   & $adb devices
   ```

   You should see a line such as `emulator-5554 device`. Then run `.\run_loopi.bat` from `E:\Loopi_Final`.

**To start the exact test emulator I used**, if `E:\Loopi\qa_avds\LoopiQA.ini` still exists, run this in a separate PowerShell terminal:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_AVD_HOME = 'E:\Loopi\qa_avds'
Start-Process -FilePath "$sdk\emulator\emulator.exe" -ArgumentList @('-avd', 'LoopiQA', '-no-snapshot', '-no-audio', '-gpu', 'swiftshader_indirect')
```

Wait for the emulator's Android home screen, then check it with `& $adb devices`. If Android Studio SDK is installed somewhere else, update `$sdk` to that location.
## 4. Build, install, and open Loopi

The easiest way is the project launcher. It builds the debug app, installs it on the first connected device, and opens Loopi:

```powershell
.\run_loopi.bat
```

You can also run each step separately:

```powershell
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
.\gradlew.bat :app:assembleDebug
& $adb install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.example.myapplication/.MainActivity
```

The APK is created at `app\build\outputs\apk\debug\app-debug.apk`.

## Optional: run the unit tests

```powershell
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
.\gradlew.bat testDebugUnitTest
```

## Troubleshooting

- **No device or emulator is running:** start one in Android Studio Device Manager, then rerun `& $adb devices`.
- **Android SDK not found:** set `$sdk` to your SDK location and set both environment variables before Gradle commands:

  ```powershell
  $sdk = 'D:\Android\Sdk' # replace with your SDK folder
  $env:ANDROID_HOME = $sdk
  $env:ANDROID_SDK_ROOT = $sdk
  ```

- **SDK component or license error:** open Android Studio → **Tools → SDK Manager**, install Platform 37 and Build-Tools 36, then accept the licenses.
- **Gradle cannot start Java:** install JDK 25 and restart VS Code so the terminal sees it. This project configures Gradle to use Java 25.
- **App builds but does not open:** confirm `& $adb devices` shows exactly the intended device as `device`, then rerun `.\run_loopi.bat`.

Use `README.md` for the flow list and local-data notes. Use `PRODUCTION_READINESS.md` for the remaining release checks.




