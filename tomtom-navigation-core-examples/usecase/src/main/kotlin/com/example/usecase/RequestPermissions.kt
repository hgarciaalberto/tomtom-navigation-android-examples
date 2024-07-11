package com.example.usecase

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions(
    permissionsList: List<String>,
    permissionsGranted: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showRationalBottomSheet by rememberSaveable { mutableStateOf(false) }

    var rationaleTitleState: Int? by rememberSaveable { mutableStateOf(null) }
    var rationaleMessageState: Int? by rememberSaveable { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val sheetState: FlexibleSheetState = rememberFlexibleBottomSheetState(
        flexibleSheetSize = FlexibleSheetSize(fullyExpanded = 0.35f),
        isModal = true,
        skipSlightlyExpanded = true,
        skipIntermediatelyExpanded = true,
    )

    // Permission state
    val multiplePermissionsState = rememberMultiplePermissionsState(permissionsList) { permissions ->
        permissions.map { permissionState ->
            Timber.d("-- permissionState: ${permissionState.key} - ${permissionState.value} --")
        }

        if (permissions.any { permission -> !permission.value }) {
            // Not all permission granted so keep showing the rationale
            Timber.d("Keep on showing rationale")
        } else {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    showRationalBottomSheet = false
                }
            }
        }
    }

    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                getRationaleTexts(
                    multiplePermissionsState,
                    permissionGranted = {
                        showRationalBottomSheet = false
                        permissionsGranted(true)
                    },
                    requestPermission = { title, message ->
                        rationaleTitleState = title
                        rationaleMessageState = message
                        showRationalBottomSheet = true
                        permissionsGranted(false)
                    },
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showRationalBottomSheet) {
        PermissionBottomSheet(
            sheetState = sheetState,
            title = stringResource(id = rationaleTitleState!!),
            message = stringResource(id = rationaleMessageState!!),
            requestPermissionClicked = {
                if (multiplePermissionsState.shouldShowRationale) {
                    multiplePermissionsState.launchMultiplePermissionRequest()
                } else {
                    showRationalBottomSheet = false
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ContextCompat.startActivity(context, intent, null)
                    }
                }
            },
            dismissClicked = {
                showRationalBottomSheet = false
                if (!multiplePermissionsState.allPermissionsGranted) {
                    scope.launch { sheetState.show() }.invokeOnCompletion {
                        if (sheetState.isVisible) {
                            showRationalBottomSheet = true
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getRationaleTexts(
    multiplePermissionsState: MultiplePermissionsState,
    permissionGranted: () -> Unit,
    requestPermission: (title: Int, message: Int) -> Unit,
) {
    if (multiplePermissionsState.allPermissionsGranted) {
        permissionGranted()
    } else {
        // Show rationale
        multiplePermissionsState.revokedPermissions.forEach { permissionState ->
            when (permissionState.permission) {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                -> {
                    val title = R.string.permission_location_title
                    val message = R.string.permission_location_rationale
                    requestPermission(title, message)
                    return@forEach
                }

                Manifest.permission.POST_NOTIFICATIONS -> {
                    val title = R.string.permission_post_notifications_title
                    val message = R.string.permission_post_notifications_rationale
                    requestPermission(title, message)
                    return@forEach
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionBottomSheet(
    sheetState: FlexibleSheetState,
    title: String,
    message: String,
    requestPermissionClicked: () -> Unit,
    dismissClicked: () -> Unit,
) {
    FlexibleBottomSheet(
        onDismissRequest = dismissClicked,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(width = 160.dp) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { requestPermissionClicked() }) {
                Text(text = stringResource(id = R.string.permission_request))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview
@Composable
private fun PermissionBottomSheetPreview() {
    MaterialTheme {
        PermissionBottomSheet(
            sheetState = rememberFlexibleBottomSheetState(
                flexibleSheetSize = FlexibleSheetSize(fullyExpanded = 0.35f),
            ),
            title = "Location Permission",
            message = "Please grant all of them for the app to work properly",
            requestPermissionClicked = {},
            dismissClicked = {},
        )
    }
}
