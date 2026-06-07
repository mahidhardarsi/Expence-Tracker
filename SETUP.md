# Setup And Run

Use these commands after cloning the repository.

## 1. Open the project folder

```powershell
cd ExpenseTracker
```

## 2. Make sure Android local SDK path exists

If `local.properties` is missing, create it with your SDK path:

```properties
sdk.dir=C:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
```

## 3. Restore or regenerate the Gradle wrapper if needed

This repo may need the wrapper jar restored before CLI builds work:

```powershell
gradle wrapper --gradle-version 8.9
```

## 4. Build the debug app

```powershell
.\gradlew.bat assembleDebug
```

## 5. Run unit tests

```powershell
.\gradlew.bat test
```

## 6. Install on a connected device or running emulator

```powershell
.\gradlew.bat installDebug
```

## 7. Build release APK

```powershell
.\gradlew.bat assembleRelease
```

## Optional: AI feature setup

If you want the on-device AI model download flow to work, add these to `gradle.properties` or your user Gradle properties file:

```properties
HF_CLIENT_ID=your_huggingface_oauth_client_id
HF_REDIRECT_URI=expensetracker://auth/huggingface
```

## Helpful checks

List connected devices:

```powershell
adb devices
```

Clean the project:

```powershell
.\gradlew.bat clean
```
