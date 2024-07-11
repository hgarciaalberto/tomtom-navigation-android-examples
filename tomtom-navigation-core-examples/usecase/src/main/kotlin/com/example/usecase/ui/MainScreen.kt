/*
 * © 2023 TomTom NV. All rights reserved.
 *
 * This software is the proprietary copyright of TomTom NV and its subsidiaries and may be
 * used for internal evaluation purposes or commercial use strictly subject to separate
 * license agreement between you and TomTom NV. If you are the licensee, you are only permitted
 * to use this software in accordance with the terms of your license agreement. If you are
 * not the licensee, you are not authorized to use this software in any manner and should
 * immediately return or destroy it.
 */
package com.example.usecase.ui

import android.Manifest
import android.app.Application
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.example.usecase.Destinations
import com.example.usecase.RequestPermissions
import com.example.usecase.databinding.ActivityBasicNavigationBinding
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.view.NavigationView

/**
 * This example shows how to build a simple navigation application using the TomTom Navigation SDK for Android.
 * The application displays a map and shows the user’s location. After the user selects a destination with a long click, the app plans a route and draws it on the map.
 * Navigation is started in a simulation mode, once the user taps on the route.
 * The application will display upcoming manoeuvres, remaining distance, estimated time of arrival (ETA), current speed, and speed limit information.
 *
 * For more details on this example, check out the tutorial: https://developer.tomtom.com/android/navigation/documentation/use-cases/build-a-navigation-app
 *
 **/
@Composable
fun MainScreen(
    navController: NavController,
) {

    val context = LocalContext.current.applicationContext

    val viewModel = viewModel<MainViewModel>(
        factory = viewModelFactory {
            initializer { MainViewModel(context as Application) }
        }
    )

    RequestPermissions(
        mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.FOREGROUND_SERVICE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(Destinations.SecondScreen().root)
                },
                content = {
                    Text("Second Screen")
                }
            )
        },
    ) { paddingValues ->
        MainContent(
            modifier = Modifier.padding(paddingValues),
            fragmentReady = { mapFragment ->
                viewModel.apply {
                    initMap(mapFragment!!)
                    initNavigationTileStore()
                    initLocationProvider()
                    initRouting()
                    initNavigation()
                }
            },
        )
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    fragmentReady: (MapFragment?) -> Unit,
) {
    var navigationFragment by remember { mutableStateOf<NavigationFragment?>(null) }
    var mapFragment by remember { mutableStateOf<MapFragment?>(null) }
    var navigationView by remember { mutableStateOf<NavigationView?>(null) }

    var tabIndex by remember { mutableStateOf(TabType.Map) }

    Column(modifier = modifier) {

        TabRow(selectedTabIndex = tabIndex.ordinal) {
            TabType.entries.forEach { item ->
                Tab(
                    text = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    selected = tabIndex == item,
                    onClick = { tabIndex = item },
                )
            }
        }


        when (tabIndex) {
            TabType.Map -> {
                AndroidViewBinding(
                    factory = { inflater: LayoutInflater, viewGroup: ViewGroup, attachToParent: Boolean ->
                        ActivityBasicNavigationBinding.inflate(inflater, viewGroup, attachToParent).apply {
//                    navigationFragment = this.navigationFragmentContainer.getFragment()
                            mapFragment = this.mapContainer.getFragment()
//                    navigationView = navigationFragment!!.navigationView

                            fragmentReady(mapFragment)

                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        // DO NOT swap or modify the background and clip because this order are mandatory.
                        .background(color = MaterialTheme.colorScheme.primary)
                        .clip(
                            RoundedCornerShape(
                                topStart = MaterialTheme.shapes.large.topStart,
                                topEnd = MaterialTheme.shapes.large.topEnd,
                                bottomEnd = CornerSize(0.dp),
                                bottomStart = CornerSize(0.dp),
                            ),
                        )
                        .height(530.dp),
                    update = { }
                )
            }

            TabType.Info -> {
                SecondScreen()
            }
        }
    }
}

enum class TabType(val title: String) {
    Map(title = "Map"),
    Info(title = "Info"),
}
