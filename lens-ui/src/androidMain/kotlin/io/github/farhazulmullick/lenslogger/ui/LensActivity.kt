@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.farhazulmullick.lenslogger.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

/**
 * Full-screen Lens inspector (network, DataStore, mocks). Opened from the persistent notification
 * after [LensAndroid.install], or via [createIntent].
 */
class LensActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LensMaterialTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .displayCutoutPadding()
                ) {
                    LensContent(LensRuntimeState.dataStores())
                }
            }
        }
    }

    companion object {
        /**
         * Explicit intent to launch the Lens inspector (same UI as the in-app bottom sheet).
         * Uses [Intent.FLAG_ACTIVITY_CLEAR_TOP] and [Intent.FLAG_ACTIVITY_SINGLE_TOP] so repeated
         * taps on the persistent notification bring an existing instance to the foreground instead
         * of stacking duplicates.
         */
        @JvmStatic
        fun createIntent(context: Context): Intent =
            Intent(context, LensActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}
