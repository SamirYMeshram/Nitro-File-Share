# Gradle sync fix

The first generated ZIP used Android Gradle Plugin `9.2.0`. Official AGP 9.2 requires Gradle `9.4.1`, so Android Studio projects that open with Gradle 8.x fail during sync.

This updated project uses:

- Android Gradle Plugin: `8.13.2`
- Kotlin Gradle Plugin: `2.3.21`
- Compose Compiler plugin: `2.3.21`
- Compose BOM: `2026.04.01`
- compileSdk / targetSdk: `36`

If sync still fails:

1. Open **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**.
2. Set **Gradle JDK** to **JDK 17**.
3. Open **SDK Manager** and install **Android API 36**.
4. Click **File > Invalidate Caches / Restart** only after verifying the versions above.

Do not add Flutter, React Native, Firebase transfer, Nearby Connections, or cloud transfer dependencies. The app remains fully native Kotlin/Android.
