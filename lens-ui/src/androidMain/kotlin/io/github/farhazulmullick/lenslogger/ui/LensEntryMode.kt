package io.github.farhazulmullick.lenslogger.ui

/**
 * How [LensAndroid] surfaces the Lens inspector on Android.
 */
enum class LensEntryMode {
    /**
     * Attaches a [androidx.compose.ui.platform.ComposeView] overlay on each activity (default, backward compatible).
     */
    WINDOW_OVERLAY,

    /**
     * Shows a persistent ongoing notification that opens [LensActivity] with the full inspector UI.
     * Does not add window overlays. On API 33+, [android.Manifest.permission.POST_NOTIFICATIONS] must be
     * granted before the notification is shown.
     */
    PERSISTENT_NOTIFICATION,
}
