# Status Blocker

Accessibility service that bounces you out of WhatsApp / WhatsApp Business status.

## Build without Android Studio
1. Create a private GitHub repo and push this folder.
2. Actions tab → "Build APK" runs automatically (or Run workflow).
3. Download the StatusBlocker-apk artifact, unzip, install the APK.

Each CI build uses a fresh debug key, so uninstall the old version before installing a new build.

## Build with Android Studio
Open the folder, let it sync (it will offer to add a Gradle wrapper), Run.

## Tuning
Constants at the top of StatusBlockService.kt: resource IDs, debounce, cover time,
and how much of the screen the Updates page must fill before it fires.
