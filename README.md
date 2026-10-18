# NewbiesTeam Android App (V1.0.0)

A high-performance native Android application for the [NewbiesTeam GT 20 Pro Hub](https://newbiesteam.surge.sh), built with modern Android standards, Material 3 adaptive theming, offline resilience, and prepared for future Kotlin Multiplatform (KMP) expansion.

---

## Key Features

* **Material 3 Adaptive Theming**: Built on `Theme.Material3.DayNight.NoActionBar` with dynamic color harmonizing (`DynamicColors`) and edge-to-edge transparent system bars for AMOLED screens (`#120d0d` & crimson `#d97757`).
* **Hardware Accelerated WebView**: Zero-latency browsing with intelligent offline fallback (`LOAD_DEFAULT` when online, `LOAD_CACHE_ELSE_NETWORK` when offline), DOM storage, and hardware layer acceleration.
* **Smart Native Navigation & Interception**:
  * Hardware back button wired to WebView history.
  * Native intent interception for ROM downloads (Google Drive, SourceForge, GitHub, Telegram, `.zip`, `.apk`, `.bin`, `.img`).
* **Background Updates (WorkManager)**:
  * Periodic background worker polling `updates.json` every 2 hours with network constraints.
  * Posts rich native notifications when a new release or announcement appears.
  * Direct deep linking to `https://newbiesteam.surge.sh/#updates`.
* **Animated Opening Splash**: Smooth crimson radial glow + scale pulse with GPU-accelerated fade-out on page finish.
* **CI/CD Cloud Builder**: Fully automated GitHub Actions workflow compiling debug & release APKs on Java 17 Temurin without requiring local Android Studio.

---

## Architecture & Kotlin Multiplatform (KMP) Roadmap

The project is structured with strict separation between pure Kotlin core business logic and Android platform APIs to ensure effortless migration to Kotlin Multiplatform (KMP) in the future:

```
android-app/
├── app/src/main/java/team/newbies/
│   ├── core/                        <-- [KMP Ready: Pure Common Logic]
│   │   ├── model/
│   │   │   ├── UpdatePost.kt        # Pure domain model for feed items
│   │   │   └── UrlRoute.kt          # Sealed route hierarchy
│   │   ├── router/
│   │   │   └── UrlRouter.kt         # Platform-independent URL classification
│   │   ├── parser/
│   │   │   └── FeedParser.kt        # Pure JSON & regex feed parser
│   │   └── repository/
│   │       └── UpdatesRepository.kt # HTTP updates fetcher & synchronizer
│   │
│   └── app/                         <-- [Android Platform Layer]
│       ├── MainActivity.kt          # Material 3 Edge-to-Edge WebView & Insets
│       ├── NewbiesApp.kt            # Application, Dynamic Colors & Channels
│       └── UpdatesWorker.kt         # WorkManager background worker
```

### Migrating to KMP:
To port this app to iOS or Compose Multiplatform:
1. Create a `:shared` KMP module.
2. Move `team.newbies.core` directly to `shared/src/commonMain/kotlin`.
3. Keep `team.newbies.app` in `androidApp/src/main/kotlin`.
4. Implement `iosApp` using the shared `UpdatesRepository` and `UrlRouter`.

---

## Building Locally or in CI

### Local Build:
```bash
./gradlew assembleDebug
```
The compiled APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions Cloud Build:
Pushing to `main` or pushing a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) automatically compiles the APK in the cloud and publishes a release artifact.
