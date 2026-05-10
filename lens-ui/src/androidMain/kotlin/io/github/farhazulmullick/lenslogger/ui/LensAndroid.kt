@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.farhazulmullick.lenslogger.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import java.util.IdentityHashMap

/**
 * Installs Lens on Android without wrapping your root composable in [LensApp].
 *
 * - [LensEntryMode.WINDOW_OVERLAY]: attaches a [ComposeView] on each activity’s content (default).
 * - [LensEntryMode.PERSISTENT_NOTIFICATION]: shows an ongoing notification that opens [LensActivity].
 *
 * Call [install] once from [Application.onCreate] on the main thread. Use [uninstall] with the same
 * [Application] instance to tear down.
 */
object LensAndroid {

    private val lock = Any()
    private var installedApplication: Application? = null
    private var callbacks: Application.ActivityLifecycleCallbacks? = null
    private val overlayByActivity = IdentityHashMap<Activity, ComposeView>()
    private var activeEntryMode: LensEntryMode? = null

    @JvmStatic
    fun install(
        application: Application,
        configuration: LensInstallConfiguration = LensInstallConfiguration(),
    ) {
        checkMainThread()
        synchronized(lock) {
            if (installedApplication != null) return
            installedApplication = application
            activeEntryMode = configuration.entryMode

            when (configuration.entryMode) {
                LensEntryMode.WINDOW_OVERLAY -> {
                    val cb = object : Application.ActivityLifecycleCallbacks {
                        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                        override fun onActivityStarted(activity: Activity) {
                            activity.window?.decorView?.post {
                                attachOverlay(activity, configuration)
                            }
                        }

                        override fun onActivityResumed(activity: Activity) {}

                        override fun onActivityPaused(activity: Activity) {}

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                        override fun onActivityDestroyed(activity: Activity) {
                            detachOverlay(activity)
                        }
                    }
                    application.registerActivityLifecycleCallbacks(cb)
                    callbacks = cb
                }

                LensEntryMode.PERSISTENT_NOTIFICATION -> {
                    callbacks = null
                    LensRuntimeState.installFrom(application, configuration)
                    LensNotificationHelper.showPersistent(application)
                }
            }
        }
    }

    /**
     * Re-shows the persistent notification after [android.Manifest.permission.POST_NOTIFICATIONS]
     * is granted (API 33+). No-op if [install] was not called with [LensEntryMode.PERSISTENT_NOTIFICATION]
     * or if [application] is not the installed instance.
     */
    @JvmStatic
    fun refreshPersistentNotification(application: Application) {
        checkMainThread()
        synchronized(lock) {
            if (installedApplication !== application) return
            if (activeEntryMode != LensEntryMode.PERSISTENT_NOTIFICATION) return
            LensNotificationHelper.showPersistent(application)
        }
    }

    /**
     * Unregisters lifecycle callbacks, removes overlay views, cancels the persistent notification (if any),
     * and clears runtime state.
     */
    @JvmStatic
    fun uninstall(application: Application) {
        checkMainThread()
        synchronized(lock) {
            if (installedApplication == null) return
            check(application === installedApplication) {
                "LensAndroid.uninstall must be called with the same Application passed to install"
            }
            callbacks?.let { application.unregisterActivityLifecycleCallbacks(it) }
            callbacks = null
            overlayByActivity.keys.toList().forEach { detachOverlay(it) }
            LensNotificationHelper.cancel(application)
            LensRuntimeState.clear()
            activeEntryMode = null
            installedApplication = null
        }
    }

    private fun attachOverlay(activity: Activity, configuration: LensInstallConfiguration) {
        if (activity.isFinishing) return
        if (!configuration.activityFilter(activity)) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        synchronized(lock) {
            if (overlayByActivity.containsKey(activity)) return
            if (content.findViewById<View>(R.id.lens_overlay_compose_view) != null) return

            val dataStores = configuration.dataStoresProvider(activity)
            val composeView = ComposeView(activity).apply {
                id = R.id.lens_overlay_compose_view
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                elevation = 24f * resources.displayMetrics.density
                setContent {
                    LensOverlayHost(
                        dataStores = dataStores,
                        showLensFAB = configuration.showLensFAB,
                        sheetGesturesEnabled = configuration.sheetGesturesEnabled,
                    )
                }
            }

            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            content.addView(composeView, lp)
            overlayByActivity[activity] = composeView
        }
    }

    private fun detachOverlay(activity: Activity) {
        val composeView: ComposeView?
        synchronized(lock) {
            composeView = overlayByActivity.remove(activity)
        }
        composeView ?: return
        val parent = composeView.parent as? ViewGroup ?: return
        parent.removeView(composeView)
    }

    private fun checkMainThread() {
        check(android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            "LensAndroid must be called on the main thread"
        }
    }
}
