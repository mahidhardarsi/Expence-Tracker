# Expense Tracker — Native Android App

A production-ready native Android application migrated from an Expo/React Native codebase.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| State | StateFlow + ViewModel |
| File I/O | Android MediaStore + SAF |
| Build | Gradle 8.9 + AGP 8.6 |

---

## Project Structure

```
app/src/main/java/com/expensetracker/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── database/     # AppDatabase + DatabaseInitializer
│   │   └── entity/       # Room entities
│   └── repository/       # ExpenseRepositoryImpl
├── di/                   # Hilt modules
├── domain/
│   ├── model/            # Domain data classes & enums
│   └── repository/       # Repository interface
├── presentation/
│   ├── ui/
│   │   ├── analysis/     # AnalysisScreen
│   │   ├── components/   # Shared Compose components
│   │   ├── options/      # OptionsScreen (categories, import/export)
│   │   ├── reports/      # ReportsListScreen, ReportDetailScreen, TransactionFormScreen
│   │   └── theme/        # AppTheme, color tokens
│   └── viewmodel/        # MainViewModel
└── util/                 # AnalysisUtil, ImportExportUtil, FileUtil, IdGenerator
```

---

## Prerequisites

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** API 29–35
- **Gradle** 8.9 (auto-downloaded via wrapper)

---

## Setup Instructions

### 1. Open in Android Studio

```
File → Open → select the ExpenseTracker/ folder
```

Android Studio will automatically:
- Sync Gradle and download dependencies
- Download the Gradle wrapper jar if missing

### 2. Sync Gradle

If prompted, click **"Sync Now"** in the banner. All dependencies are resolved from Maven Central and Google Maven.

### 3. Run on Device / Emulator

- Connect an Android device (API 29+) or start an emulator
- Click the ▶ **Run** button or press `Shift+F10`

### 4. Build a Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

To sign for Play Store:
```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

---

## Features Migrated

| Expo Feature | Native Android Implementation |
|---|---|
| `expo-sqlite` | Room (SQLite wrapper) |
| `zustand` state | ViewModel + StateFlow |
| `expo-document-picker` | `ActivityResultContracts.GetContent()` |
| `expo-file-system` (SAF) | MediaStore API (API 29+) |
| `expo-sharing` | Android Share Sheet + FileProvider |
| `expo-haptics` | `VibrationEffect` API |
| `react-native-gifted-charts` | Custom Compose bar/pie charts |
| Bottom tab navigation | Material 3 `NavigationBar` |
| Light/Dark theme | `MaterialTheme` + `CompositionLocalProvider` |
| JSON/CSV import | Gson + custom CSV parser |
| JSON/CSV export | MediaStore → Downloads/ExpenseTracker/ |

---

## Architecture Notes

### Clean Architecture Layers

1. **Domain** — Pure Kotlin models + repository interface. Zero Android imports.
2. **Data** — Room entities, DAOs, `ExpenseRepositoryImpl`. Maps between DB entities and domain models.
3. **Presentation** — Compose screens, `MainViewModel` (single ViewModel for the whole app, matching the Zustand store pattern from the original).

### State Management

The original app used a single flat Zustand store. This is mirrored with a single `MainViewModel` + `AppUiState` data class. `StateFlow` provides reactive updates to all Compose screens. This avoids the complexity of multiple ViewModels sharing state for an app of this scope.

### Navigation

The original app used a custom stack-based navigator within a single component. This is replicated with plain `var` state variables in `AppRoot` (tab + sub-route), matching the exact same navigation model. No NavGraph is used since the app is fully single-activity with in-memory navigation — avoiding the complexity of NavHost argument serialization for this scope.

### Database Initialization

`DatabaseInitializer` seeds default categories, creates the initial report, and normalizes `custom_order` on first launch — matching the `initializeDatabase()` logic from the Expo app exactly.

### ID Generation

`IdGenerator.newId()` = `System.currentTimeMillis() + random(0..999)` — identical to the Expo app's `createNumericId()` timestamp strategy.

### File Export

- **Android 10+**: Uses `MediaStore.Downloads` API — no permissions required
- Files saved to `Downloads/ExpenseTracker/`
- Share via Android share sheet using `FileProvider` + `Intent.ACTION_SEND`

---

## Known Limitations

1. **Charts**: The Expo app used `react-native-gifted-charts` for animated pie/bar charts. The native version uses simple custom Compose bar charts. For richer charts, integrate [Vico](https://github.com/patrykandpatrick/vico) (dependency already included in `build.gradle.kts`) — see `vico-compose-m3` in the dependency catalog.

2. **Haptics**: The Expo app used `expo-haptics`. Android haptics (`VibrationEffect`) are straightforward to add — inject a `HapticFeedbackManager` and call it on button taps. Omitted from initial migration to keep scope clean.

3. **Gradle Wrapper JAR**: The `gradle-wrapper.jar` binary is not included (GitHub blocks binary distribution). Android Studio will auto-download it on first sync, or run:
   ```bash
   gradle wrapper --gradle-version=8.9
   ```

4. **Signing**: Release builds require a keystore. See [Android signing docs](https://developer.android.com/studio/publish/app-signing).

5. **Analysis custom date range**: The "Custom Range" period filter UI has date field inputs but no date picker dialog — add `DatePickerDialog` calls matching the transaction form pattern.

---

## Dependency Catalog

All versions are in `gradle/libs.versions.toml`:

```
AGP 8.6.1 | Kotlin 2.0.21 | Compose BOM 2024.12.01
Hilt 2.52 | Room 2.6.1 | DataStore 1.1.1
Navigation 2.8.5 | Coroutines 1.9.0
Gson 2.11.0 | Vico 2.0.0-beta.1
WorkManager 2.10.0 | Splashscreen 1.0.1
```

---

## AI Setup

The app now includes an on-device AI feature set split across `:core:model`, `:core:finance`, and `:feature:ai`.

To enable the Hugging Face model download flow, provide these Gradle properties before syncing:

```properties
HF_CLIENT_ID=your_huggingface_oauth_client_id
HF_REDIRECT_URI=expensetracker://auth/huggingface
```

Notes:
- Configure the Hugging Face OAuth app as a public/native client.
- The redirect URI must exactly match the manifest deep link.
- Downloaded `.litertlm` files are stored in app-private storage under `filesDir/ai-models/`.
- Expect roughly 0.9 GB to 1.4 GB disk usage per model and substantially higher RAM usage while loaded.

---

## Running Tests

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

---

## Play Store Checklist

- [ ] Set `applicationId` to your unique package name in `app/build.gradle.kts`
- [ ] Increment `versionCode` and `versionName` for each release
- [ ] Create and configure a release keystore
- [ ] Add Firebase (optional) for crash reporting
- [ ] Test on API 29 (minimum) and API 35 (latest)
- [ ] Generate AAB: `./gradlew bundleRelease`
