# LensLogger
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Android-supported-brightgreen?logo=android)
![Platform](https://img.shields.io/badge/iOS-supported-lightgrey?logo=apple)
![Platform](https://img.shields.io/badge/Desktop-supported-blue?logo=windows)

LensLogger is a Kotlin Multiplatform (KMP) library for Android, iOS and Desktop that makes debugging network requests effortless.
It automatically logs all Ktor network requests and responses, and provides a built-in UI to inspect these logs directly in your app. 
This helps you quickly identify issues and monitor network activity during development.

## Features
- ✨ Seamless integration with Ktor HTTP client
- 📱 Works on both Android and iOS (KMP)
- 🔍 Logs all network requests and responses
- 🖥️ Built-in UI for real-time log inspection
- 🛠️ Minimal setup and easy to use
- ✨ DataStore Visualizer

## Demo

| Android                                                                       | iOS                                                                       | Desktop (Windows)                                                            |
|-------------------------------------------------------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------|
| <img src="assets/android_logger_demo.gif" width="240" alt="Lens Logger Demo"> | <img src="assets/ios_logger_demo.gif" width="200" alt="Lens Logger Demo"> |<img src="assets/desktop_logger_demo.gif" width="500" alt="Lens Logger Demo">
| <img src="assets/datastore_demo.gif" width="240" alt="Lens Logger Demo">


## Installation

Add the LensLogger artifact to your module's commonMain dependencies:

```kotlin
dependencies {
    implementation("io.github.farhazulmullick:lens-logger:<version>")
}
```
Or add to your `libs.versions.toml`:
```toml
lensLoggerVersion = "<version>"
lens-logger = { module = "io.github.farhazulmullick:lens-logger", version.ref = "lensLoggerVersion" }
```

### Publish to Maven Local (try in another project)

From this repository root:

```bash
./gradlew publishToMavenLocal
```

Artifacts use `groupId` **`io.github.farhazulmullick`**, current `version` **`1.2.0-SNAPSHOT`** (see each module’s `build.gradle.kts` under `mavenPublishing { coordinates(...) }`). They are installed under `~/.m2/repository/io/github/farhazulmullick/`.

**In a separate Android (or KMP) project**, add Maven Local and the dependency.

`settings.gradle.kts` (top-level `dependencyManagement` / `pluginManagement` repositories, or `dependencyResolutionManagement.repositories`):

```kotlin
mavenLocal()
```

`app/build.gradle.kts` (or your shared `commonMain` source set for KMP):

```kotlin
dependencies {
    implementation("io.github.farhazulmullick:lens-logger:1.2.0-SNAPSHOT")
}
```

Your app must already use **Jetpack Compose** (and compatible Kotlin / Compose Compiler versions) because `lens-ui` is Compose-based. After changing the library, run `publishToMavenLocal` again and **Sync** / **Refresh dependencies** in the consumer so Gradle picks up the new snapshot.

## Usage

### 1. Integrate with Ktor Client

In your shared code (e.g., `commonMain`):

```kotlin
import io.github.farhazulmullick.lenslogger.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.logging.*

val client = HttpClient(engine) {
    // Replace install(Logging) with this.
    // Log request/response in Logcat and LensUi as well.
    LensHttpLogger {
        level = LogLevel.ALL
        logger = object : Logger {
            override fun log(message: String) {
                Napier.d(message = message)
            }
        }
    }.also { 
        // setup up nappier logger.
        Napier.base(DebugAntilog()) 
    }
}

/******************* OR ********************/
/** Install only LensLogger **/

val client = HttpClient(engine) {
    // body 
    install(LensHttpLogger){
        level = LogLevel.ALL
    }
}

```

### 2. Lens UI (Compose) and Android without a Compose root

**Compose root:** wrap your app with `LensApp`. It draws a draggable `LensFAB` that opens `LensBottomSheet` with the inspector (`LensContent`). Pass `dataStores` for the DataStore tab.

```kotlin
import io.github.farhazulmullick.lenslogger.ui.LensApp
import androidx.compose.ui.Modifier

LensApp(
    modifier = Modifier.fillMaxSize(),
    showLensFAB = true,
    sheetGesturesEnabled = false,
    dataStores = myDataStores,
) {
    App()
}
```

> **Note:** There is **no** window-level `ComposeView` overlay on activities; the FAB lives in your Compose tree only.

**Android, no Compose root:** `LensAndroid.install` posts a **persistent notification** that opens **`LensActivity`** (full-screen inspector). `LensInstallConfiguration` only needs **`dataStoresProvider`**.

```kotlin
import android.app.Application
import io.github.farhazulmullick.lenslogger.ui.LensAndroid
import io.github.farhazulmullick.lenslogger.ui.LensInstallConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LensAndroid.install(
            this,
            LensInstallConfiguration(dataStoresProvider = { ctx -> emptyList() }),
        )
    }
}
```

Register `android:name=".MyApp"`. Call **`LensAndroid.uninstall(this)`** to cancel the notification and clear cached DataStores. **`LensAndroid.refreshPersistentNotification(application)`** after granting **`POST_NOTIFICATIONS`** (API 33+) if the notification was skipped at install.

**iOS:** embed the inspector in your own UI or navigation; notification entry is not provided here.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
