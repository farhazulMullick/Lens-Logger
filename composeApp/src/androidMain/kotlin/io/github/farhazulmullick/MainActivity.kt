package io.github.farhazulmullick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.farhazulmullick.datastore.STORE_FILE_1
import io.github.farhazulmullick.datastore.STORE_FILE_2
import io.github.farhazulmullick.datastore.STORE_FILE_3
import io.github.farhazulmullick.datastore.createDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dataStores = listOf(
            createDataStore { filesDir.resolve(STORE_FILE_1).absolutePath },
            createDataStore { filesDir.resolve(STORE_FILE_2).absolutePath },
            createDataStore { filesDir.resolve(STORE_FILE_3).absolutePath },
        )


        setContent {
            MaterialTheme(
                colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme())
                    androidx.compose.material3.darkColorScheme()
                else
                    androidx.compose.material3.lightColorScheme()
            ) {
                App(stores = dataStores)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}