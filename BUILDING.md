# Building Tablo Multiview

This document outlines how to compile and build the Tablo Multiview APK from source.

## Prerequisites

- **JDK 17** or newer
- **Android SDK Platform 36** (Compile SDK: 36, Target SDK: 36, Min SDK: 24)
- **Gradle 8.11+** with Kotlin DSL support
- Android Build Tools

## Building the Debug APK

To compile and produce a debug APK for sideloading:

```bash
gradle :app:assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Running Local Unit Tests

To run local unit and Robolectric tests:

```bash
gradle :app:testDebugUnitTest
```

## Sideloading to Amazon Fire TV

1. Ensure your computer and Fire TV are connected to the same Wi-Fi network.
2. Note your Fire TV's IP address from **Settings → My Fire TV → About → Network**.
3. Connect using ADB:
   ```bash
   adb connect <FIRE_TV_IP>:5555
   ```
4. Sideload the APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. Launch the application:
   ```bash
   adb shell am start -n com.aistudio.tablomultiview.tvapp/com.example.MainActivity
   ```
