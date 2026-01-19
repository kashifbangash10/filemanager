---
description: Build, install and launch the Android application on a connected device.
---

// turbo-all
1. Set the necessary environment variables and run the Gradle install task.
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="C:\Users\USER\AppData\Local\Android\Sdk"
./gradlew installDebug
```

2. Launch the main activity using ADB.
```powershell
& "C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.nextguidance.filesexplorer.filemanager.smartfiles/.SplashActivity
```
