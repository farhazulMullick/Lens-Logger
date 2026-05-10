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
 * Installs a per-activity [ComposeView] hosting [LensOverlayHost] on the activity's
 * [android.R.id.content] [FrameLayout], so the Lens FAB is available without wrapping
 * your root composable in [LensApp].
 *
 * Call [install] once from [Application.onCreate] on the main thread. Use
 * [LensInstallConfiguration.activityFilter] to exclude specific activities.
 *
 * This does not use [android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]; the
 * overlay stays within your app's window.
 */
object LensAndroid {

    private val lock = Any()
    private var installedApplication: Application? = null
    private var callbacks: Application.ActivityLifecycleCallbacks? = null
    private val overlayByActivity = IdentityHashMap<Activity, ComposeView>()

    @JvmStatic
    fun install(
        application: Application,
        configuration: LensInstallConfiguration = LensInstallConfiguration(),
    ) {
        checkMainThread()
        synchronized(lock) {
            if (callbacks != null) return
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
            installedApplication = application
        }
    }

    /**
     * Unregisters lifecycle callbacks and removes any overlay views still attached.
     */
    @JvmStatic
    fun uninstall(application: Application) {
        checkMainThread()
        synchronized(lock) {
            val cb = callbacks ?: return
            check(application === installedApplication) {
                "LensAndroid.uninstall must be called with the same Application passed to install"
            }
            application.unregisterActivityLifecycleCallbacks(cb)
            callbacks = null
            installedApplication = null
            overlayByActivity.keys.toList().forEach { detachOverlay(it) }
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
