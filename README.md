# LensLogger

LensLogger is a Kotlin Multiplatform (KMP) library for Android and iOS that makes debugging network requests effortless. It automatically logs all Ktor network requests and responses, and provides a built-in UI to inspect these logs directly in your app. This helps you quickly identify issues and monitor network activity during development.

## Features
- ✨ Seamless integration with Ktor HTTP client
- 📱 Works on both Android and iOS (KMP)
- 🔍 Logs all network requests and responses
- 🖥️ Built-in UI for real-time log inspection
- 🛠️ Minimal setup and easy to use

## Demo

<img src="assets/lens_logger_demo.gif" width="180" alt="Lens Logger Demo">

## Installation

Add the LensLogger artifact to your module's commonMain dependencies:

```kotlin
dependencies {
    implementation("io.github.farhazulmullick:lens-logger:0.0.1-alpha01")
}
```
Or add to your `libs.versions.toml`:
```toml
lensVersion = "0.0.1-alpha01"
lens-logger = { module = "io.github.farhazulmullick:lens-logger", version.ref = "lensVersion" }
```

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
```

### 2. Setup LensApp UI

Simply wrap your app's root composable with `LensApp`. This will enable the LensLogger UI and log request/response in your app.


```kotlin
import io.github.farhazulmullick.lenslogger.ui.LensApp
import androidx.compose.ui.Modifier

LensApp(
    modifier = Modifier.fillMaxSize(), 
    // by default enabled, set to false to disable.
    showLensFAB = true
) {
    // Your app content goes here
    App()
}
```

This will display your app content and allow you to open the LensLogger UI overlay for network log inspection.

> **Note:** Make sure you have set up LensLogger with your Ktor client as shown above in your network module.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
