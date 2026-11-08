# NewbiesTeam Android App

The official Android app for the [NewbiesTeam GT 20 Pro Hub](https://newbiesteam.surge.sh).

It wraps the website in an Android WebView so you can browse ROMs, kernels, recovery tools, and guides directly from your phone.

---

## What it does

* **Offline Caching**: If you have internet it loads fresh content. If you're offline, it loads from cache so you can still read guides.
* **External Downloads**: Download links (Google Drive, SourceForge, GitHub, Mega, MediaFire, Telegram, and direct `.zip` or `.apk` files) open in your browser or default app instead of breaking inside the WebView.
* **Back Button Navigation**: Pressing your phone's back button navigates back through pages before closing the app.
* **Pull to Refresh**: Swipe down from the top to reload the page.
* **Background Notifications**: Uses WorkManager to check `updates.json` every couple of hours. When someone posts a new ROM or update, you get a phone notification. Tapping it takes you straight to the updates tab.

---



## Release Signing

Release builds are signed with a keystore injected by GitHub Actions from encrypted repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). The keystore is never committed.

For local release builds, place the keystore at `app/release.jks` (gitignored) and export the same environment variables before running `./gradlew assembleRelease`.

> **Signing-key rotation:** a build signed with a new key cannot update an install signed with the old key. Uninstall the old app first, then install the new build.

If Google Play Protect shows an "Unrecognized app" popup on fresh installs, choose **More details → Install anyway**.


