package com.example.books_kmp.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraPermissionHandler(
    val state: CameraPermissionState,
    val request: () -> Unit,
    val openSettings: () -> Unit,
    val reset: () -> Unit,
)

@Composable
fun rememberCameraPermissionHandler(): CameraPermissionHandler {
    val context = LocalContext.current
    var state by remember { mutableStateOf<CameraPermissionState>(CameraPermissionState.Idle) }

    val activity = remember(context) { context.findActivity() }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val shouldShowRationale =
                activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
            state = CameraPermissionState.fromResult(granted = granted, shouldShowRationale = shouldShowRationale)
        }

    return remember(state) {
        CameraPermissionHandler(
            state = state,
            request = {
                val alreadyGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                if (alreadyGranted) {
                    state = CameraPermissionState.Granted
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
            openSettings = {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            },
            reset = { state = CameraPermissionState.Idle },
        )
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
