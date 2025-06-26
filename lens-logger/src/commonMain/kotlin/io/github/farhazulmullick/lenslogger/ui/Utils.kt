package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.ktor.http.HttpStatusCode

@Composable
fun HSpacer(dp: Dp) {
    Spacer(modifier = Modifier.width(dp))
}

@Composable
fun VSpacer(dp: Dp) {
    Spacer(modifier = Modifier.height(dp))
}

@Composable
fun StatusCodeIcon(statusCode: HttpStatusCode?) {
    when(statusCode?.value) {
        in 200..299 ->{
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        in 300..399 -> {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = "Redirect",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        in 400..499 -> {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Client Error",
                tint = MaterialTheme.colorScheme.error
            )
        }

        in 500..599 -> {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Server Error",
                tint = MaterialTheme.colorScheme.error
            )
        }

        else -> {}
    }
}