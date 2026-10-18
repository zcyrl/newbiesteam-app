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



## Release Signing & Play Protect

The app is signed with a permanent release keystore (`app/release.jks`, alias `newbiesteam`) so that:
* Every build uses the exact same signature, so users can install future updates without uninstalling.
* Play Protect remembers your key signature.

If Google Play Protect shows an "Unrecognized app" popup on fresh installs:
1. Tap **More details** -> **Install anyway**.
2. Once installed, future updates signed with this key install without warnings.


