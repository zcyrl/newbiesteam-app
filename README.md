# NewbiesTeam Android App

The official Android app for the [NewbiesTeam GT 20 Pro Hub](https://newbiesteam.surge.sh).

It wraps the website in an Android WebView so you can browse ROMs, kernels, recovery tools, and guides directly from your phone.

---

## What it does

* **Dark Theme**: Matches the site's dark AMOLED background (`#120d0d`) and crimson colors (`#d97757`).
* **Offline Caching**: If you have internet it loads fresh content. If you're offline, it loads from cache so you can still read guides.
* **External Downloads**: Download links (Google Drive, SourceForge, GitHub, Mega, MediaFire, Telegram, and direct `.zip` or `.apk` files) open in your browser or default app instead of breaking inside the WebView.
* **Back Button Navigation**: Pressing your phone's back button navigates back through pages before closing the app.
* **Pull to Refresh**: Swipe down from the top to reload the page.
* **Background Notifications**: Uses WorkManager to check `updates.json` every couple of hours. When someone posts a new ROM or update, you get a phone notification. Tapping it takes you straight to the updates tab.

---

## Project Structure

The code is split so business logic stays separate from Android UI. That way, if we want to make an iOS version with Kotlin Multiplatform (KMP) later, the core logic can be reused without rewriting it.

```
app/src/main/java/team/newbies/
├── core/                        # Pure Kotlin (No Android imports, KMP ready)
│   ├── model/UpdatePost.kt      # Post data model
│   ├── model/UrlRoute.kt        # Route types (internal, external, download)
│   ├── router/UrlRouter.kt      # Decides which links open inside vs outside
│   ├── parser/FeedParser.kt     # Parses updates.json and data.js
│   └── repository/UpdatesRepository.kt # Fetches the latest updates from the web
│
└── app/                         # Android-specific code
    ├── MainActivity.kt          # WebView setup, back button, edge-to-edge
    ├── NewbiesApp.kt            # App startup and notification channel setup
    └── UpdatesWorker.kt         # Background worker for update checks
```

---

## How to Build

### Local
Make sure you have JDK 17 installed, then run:

```bash
./gradlew assembleDebug
```

The APK will be saved to:
`app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions
Every push to `main` builds automatically on GitHub Actions.

To make a new release with a downloadable APK on GitHub:
```bash
git tag v1.0.1
git push origin v1.0.1
```
This triggers the workflow to compile the APK and attach it to a GitHub Release.

---

## Release Signing & Play Protect

The app is signed with a permanent release keystore (`app/release.jks`, alias `newbiesteam`) so that:
* Every build uses the exact same signature, so users can install future updates without uninstalling.
* Play Protect remembers your key signature.

If Google Play Protect shows an "Unrecognized app" popup on fresh installs:
1. Tap **More details** -> **Install anyway**.
2. Once installed, future updates signed with this key install without warnings.
3. You can also submit the signed APK to [Google Play Protect Developer Form](https://support.google.com/googleplay/android-developer/contact/protectappeals) with your certificate SHA-256 (printed in the GitHub Actions build log) to whitelist it permanently.

