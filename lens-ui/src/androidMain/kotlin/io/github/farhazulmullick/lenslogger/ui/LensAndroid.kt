package io.github.farhazulmullick.lenslogger.ui

import android.app.Application

/**
 * Installs Lens on Android when the app has **no** root composable to wrap with [LensApp]:
 * shows a **persistent notification** that opens [LensActivity].
 *
 * If you use Compose at the root, prefer [LensApp] (FAB + sheet) instead of this helper.
 *
 * Call [install] once from [Application.onCreate] on the main thread. Use [uninstall] with the same
 * [Application] instance to cancel the notification and clear runtime state.
 */
object LensAndroid {

    private val lock = Any()
    private var installedApplication: Application? = null

    @JvmStatic
    fun install(
        application: Application,
        configuration: LensInstallConfiguration = LensInstallConfiguration(),
    ) {
        checkMainThread()
        synchronized(lock) {
            if (installedApplication != null) return
            installedApplication = application
            LensRuntimeState.installFrom(application, configuration)
            LensNotificationHelper.showPersistent(application)
        }
    }

    /**
     * Re-shows the persistent notification after [android.Manifest.permission.POST_NOTIFICATIONS]
     * is granted (API 33+). No-op if [install] was not called or [application] is not the installed instance.
     */
    @JvmStatic
    fun refreshPersistentNotification(application: Application) {
        checkMainThread()
        synchronized(lock) {
            if (installedApplication !== application) return
            LensNotificationHelper.showPersistent(application)
        }
    }

    /**
     * Cancels the persistent notification and clears runtime state (including cached DataStores for [LensActivity]).
     */
    @JvmStatic
    fun uninstall(application: Application) {
        checkMainThread()
        synchronized(lock) {
            if (installedApplication == null) return
            check(application === installedApplication) {
                "LensAndroid.uninstall must be called with the same Application passed to install"
            }
            LensNotificationHelper.cancel(application)
            LensRuntimeState.clear()
            installedApplication = null
        }
    }

    private fun checkMainThread() {
        check(android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            "LensAndroid must be called on the main thread"
        }
    }
}
