package com.example.usecase.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.usecase.BuildConfig
import com.example.usecase.databinding.ActivityBasicNavigationBinding
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStore
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStoreConfiguration
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationProvider
import com.tomtom.sdk.location.simulation.SimulationLocationProvider
import com.tomtom.sdk.location.simulation.strategy.InterpolationStrategy
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraChangeListener
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.gesture.MapLongClickListener
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteClickListener
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.currentlocation.CurrentLocationButton.VisibilityPolicy
import com.tomtom.sdk.navigation.ActiveRouteChangedListener
import com.tomtom.sdk.navigation.ProgressUpdatedListener
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.online.Configuration
import com.tomtom.sdk.navigation.online.OnlineTomTomNavigationFactory
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.ExtendedSections
import com.tomtom.sdk.routing.options.guidance.GuidanceOptions
import com.tomtom.sdk.routing.options.guidance.InstructionPhoneticsType
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.vehicle.Vehicle
import com.tomtom.sdk.vehicle.VehicleProviderFactory
import timber.log.Timber

class MainViewModel(val context: Application) : AndroidViewModel(context) {

    private lateinit var binding: ActivityBasicNavigationBinding
    private lateinit var mapFragment: MapFragment
    private lateinit var tomTomMap: TomTomMap
    private lateinit var navigationTileStore: NavigationTileStore
    private lateinit var locationProvider: LocationProvider
    private lateinit var onLocationUpdateListener: OnLocationUpdateListener
    private lateinit var routePlanner: RoutePlanner
    private var route: Route? = null
    private lateinit var routePlanningOptions: RoutePlanningOptions
    private lateinit var tomTomNavigation: TomTomNavigation
    private lateinit var navigationFragment: NavigationFragment

    val apiKey = BuildConfig.TOMTOM_API_KEY

    /**
     * Displaying a map
     *
     * MapOptions is required to initialize the map.
     * Use MapFragment to display a map.
     * Optional configuration: You can further configure the map by setting various properties of the MapOptions object. You can learn more in the Map Configuration guide.
     * The last step is adding the MapFragment to the previously created container.
     */
    fun initMap(mapFragment: MapFragment) {
//        val mapOptions = MapOptions(mapKey = apiKey)
//        this.mapFragment = MapFragment.newInstance(mapOptions)
        this.mapFragment = mapFragment
//        context.supportFragmentManager.beginTransaction()
//            .replace(R.id.map_container, mapFragment)
//            .commit()
        this.mapFragment.getMapAsync { map ->
            tomTomMap = map
            enableUserLocation()
            setUpMapListeners()
        }
    }

    /**
     * The SDK provides a [NavigationTileStore] class that is used between different modules to get tile data based
     * on the online map.
     */
    fun initNavigationTileStore() {
        navigationTileStore = NavigationTileStore.create(
            context = context,
            navigationTileStoreConfig = NavigationTileStoreConfiguration(
                apiKey = apiKey
            )
        )
    }


    /**
     * The SDK provides a [LocationProvider] interface that is used between different modules to get location updates.
     * This examples uses the [AndroidLocationProvider].
     * Under the hood, the engine uses Android’s system location services.
     */
    fun initLocationProvider() {
        locationProvider = AndroidLocationProvider(context = context)
    }

    /**
     * You can plan route by initializing by using the online route planner and default route replanner.
     */
    fun initRouting() {
        routePlanner = OnlineRoutePlanner.create(context = context, apiKey = apiKey)
    }

    /**
     * To use navigation in the application, start by by initialising the navigation configuration.
     */
    fun initNavigation() {
        val configuration = Configuration(
            context = context,
            navigationTileStore = navigationTileStore,
            locationProvider = locationProvider,
            routePlanner = routePlanner,
            vehicleProvider = VehicleProviderFactory.create(vehicle = Vehicle.Car())
        )
        tomTomNavigation = OnlineTomTomNavigationFactory.create(configuration)
    }

    /**
     * In order to show the user’s location, the application must use the device’s location services, which requires the appropriate permissions.
     */
    private fun enableUserLocation() {
//        if (areLocationPermissionsGranted()) {
        showUserLocation()
//        } else {
//            requestLocationPermission()
//        }
    }

    /**
     * The LocationProvider itself only reports location changes. It does not interact internally with the map or navigation.
     * Therefore, to show the user’s location on the map you have to set the LocationProvider to the TomTomMap.
     * You also have to manually enable the location indicator.
     * It can be configured using the LocationMarkerOptions class.
     *
     * Read more about user location on the map in the Showing User Location guide.
     */
    private fun showUserLocation() {
        locationProvider.enable()
        // zoom to current location at city level
        onLocationUpdateListener = OnLocationUpdateListener { location ->
            tomTomMap.moveCamera(CameraOptions(location.position, zoom = 8.0))
            locationProvider.removeOnLocationUpdateListener(onLocationUpdateListener)
        }
        locationProvider.addOnLocationUpdateListener(onLocationUpdateListener)
        tomTomMap.setLocationProvider(locationProvider)
        val locationMarker = LocationMarkerOptions(type = LocationMarkerOptions.Type.Pointer)
        tomTomMap.enableLocationMarker(locationMarker)
    }

    /**
     * In this example on planning a route, the origin is the user’s location and the destination is determined by the user selecting a location on the map.
     * Navigation is started once the user taps on the route.
     *
     * To mark the destination on the map, add the MapLongClickListener event handler to the map view.
     * To start navigation, add the addRouteClickListener event handler to the map view.
     */
    private fun setUpMapListeners() {
        tomTomMap.addMapLongClickListener(mapLongClickListener)
        tomTomMap.addRouteClickListener(routeClickListener)
    }

    /**
     * Used to calculate a route based on a selected location.
     * - The method removes all polygons, circles, routes, and markers that were previously added to the map.
     * - It then creates a route between the user’s location and the selected location.
     * - The method needs to return a boolean value when the callback is consumed.
     */
    private val mapLongClickListener = MapLongClickListener { geoPoint ->
        clearMap()
        calculateRouteTo(geoPoint)
        true
    }

    /**
     * Checks whether navigation is currently running.
     */
    private fun isNavigationRunning(): Boolean = tomTomNavigation.navigationSnapshot != null


    /**
     * Used to start navigation based on a tapped route, if navigation is not already running.
     * - Hide the location button
     * - Then start the navigation using the selected route.
     */
    private val routeClickListener = RouteClickListener {
        if (!isNavigationRunning()) {
            route?.let { route ->
                mapFragment.currentLocationButton.visibilityPolicy = VisibilityPolicy.Invisible
                startNavigation(route)
            }
        }
    }

    /**
     * Used to calculate a route using the following parameters:
     * - InstructionType - This indicates that the routing result has to contain guidance instructions.
     * - InstructionPhoneticsType - This specifies whether to include phonetic transcriptions in the response.
     * - AnnouncementPoints - When this parameter is specified, the instruction in the response includes up to three additional fine-grained announcement points, each with its own location, maneuver type, and distance to the instruction point.
     * - ExtendedSections - This specifies whether to include extended guidance sections in the response, such as sections of type road shield, lane, and speed limit.
     * - ProgressPoints - This specifies whether to include progress points in the response.
     */
    private fun calculateRouteTo(destination: GeoPoint) {
        val userLocation =
            tomTomMap.currentLocation?.position ?: return
        val itinerary = Itinerary(origin = userLocation, destination = destination)
        routePlanningOptions = RoutePlanningOptions(
            itinerary = itinerary,
            guidanceOptions = GuidanceOptions(
                phoneticsType = InstructionPhoneticsType.Ipa,
                extendedSections = ExtendedSections.All,
            ),
            vehicle = Vehicle.Car()
        )
        routePlanner.planRoute(routePlanningOptions, routePlanningCallback)
    }

    /**
     * The RoutePlanningCallback itself has two methods.
     * - The first method is triggered if the request fails.
     * - The second method returns RoutePlanningResponse containing the routing results.
     * - This example draws the first retrieved route on the map, using the RouteOptions class.
     */
    private val routePlanningCallback = object : RoutePlanningCallback {
        override fun onSuccess(result: RoutePlanningResponse) {
            route = result.routes.first()
            drawRoute(route!!)
            tomTomMap.zoomToRoutes(ZOOM_TO_ROUTE_PADDING)
        }

        override fun onFailure(failure: RoutingFailure) {
            Timber.w(failure.message)
            Toast.makeText(context, failure.message, Toast.LENGTH_SHORT).show()
        }

        override fun onRoutePlanned(route: Route) = Unit
    }

    /**
     * Used to draw route on the map
     * You can show the overview of the added routes using the TomTomMap.zoomToRoutes(Int) method. Note that its padding parameter is expressed in pixels.
     */
    private fun drawRoute(route: Route) {
        val instructions = route.mapInstructions()
        val routeOptions = RouteOptions(
            geometry = route.geometry,
            destinationMarkerVisible = true,
            departureMarkerVisible = true,
            instructions = instructions,
            routeOffset = route.routePoints.map { it.routeOffset }
        )
        tomTomMap.addRoute(routeOptions)
    }

    /**
     * For the navigation use case, the instructions can be drawn on the route in form of arrows that indicate maneuvers.
     * To do this, map the Instruction object provided by routing to the Instruction object used by the map.
     * Note that during navigation, you need to update the progress property of the drawn route to display the next instructions.
     */
    private fun Route.mapInstructions(): List<Instruction> {
        val routeInstructions = legs.flatMap { routeLeg -> routeLeg.instructions }
        return routeInstructions.map {
            Instruction(
                routeOffset = it.routeOffset,
            )
        }
    }

    /**
     * Used to start navigation by
     * - initializing the NavigationFragment to display the UI navigation information,
     * - passing the Route object along which the navigation will be done, and RoutePlanningOptions used during the route planning,
     * - handling the updates to the navigation states using the NavigationListener.
     * Note that you have to set the previously-created TomTom Navigation object to the NavigationFragment before using it.
     */

    private fun startNavigation(route: Route) {
        initNavigationFragment()
        navigationFragment.setTomTomNavigation(tomTomNavigation)
        val routePlan = RoutePlan(route, routePlanningOptions)
        navigationFragment.startNavigation(routePlan)
        navigationFragment.addNavigationListener(navigationListener)
        tomTomNavigation.addProgressUpdatedListener(progressUpdatedListener)
        tomTomNavigation.addActiveRouteChangedListener(activeRouteChangedListener)
    }

    /**
     * Handle the updates to the navigation states using the NavigationListener
     * - Use CameraChangeListener to observe camera tracking mode and detect if the camera is locked on the chevron. If the user starts to move the camera, it will change and you can adjust the UI to suit.
     * - Use the SimulationLocationProvider for testing purposes.
     * - Once navigation is started, the camera is set to follow the user position, and the location indicator is changed to a chevron. To match raw location updates to the routes, use MapMatchedLocationProvider and set it to the TomTomMap.
     * - Set the bottom padding on the map. The padding sets a safe area of the MapView in which user interaction is not received. It is used to uncover the chevron in the navigation panel.
     */
    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.addCameraChangeListener(cameraChangeListener)
            tomTomMap.cameraTrackingMode = CameraTrackingMode.FollowRouteDirection
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Chevron))
            setMapMatchedLocationProvider()
            setSimulationLocationProviderToNavigation(route!!)
            setMapNavigationPadding()
        }

        override fun onStopped() {
            stopNavigation()
        }
    }

    /**
     * Used to initialize the NavigationFragment to display the UI navigation information,
     */
    private fun initNavigationFragment() {
        if (!::navigationFragment.isInitialized) {
            navigationFragment = NavigationFragment.newInstance(
                NavigationUiOptions(
                    keepInBackground = true
                )
            )
        }
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.navigation_fragment_container, navigationFragment)
//            .commitNow()
    }

    private val progressUpdatedListener = ProgressUpdatedListener {
        tomTomMap.routes.first().progress = it.distanceAlongRoute
    }

    private val activeRouteChangedListener by lazy {
        ActiveRouteChangedListener { route ->
            tomTomMap.removeRoutes()
            drawRoute(route)
        }
    }

    /**
     * Use the SimulationLocationProvider for testing purposes.
     */
    private fun setSimulationLocationProviderToNavigation(route: Route) {
        val routeGeoLocations = route.geometry.map { GeoLocation(it) }
        val simulationStrategy = InterpolationStrategy(routeGeoLocations)
        locationProvider = SimulationLocationProvider.create(strategy = simulationStrategy)
        tomTomNavigation.locationProvider = locationProvider
        locationProvider.enable()
    }

    /**
     * Stop the navigation process using NavigationFragment.
     * This hides the UI elements and calls the TomTomNavigation.stop() method.
     * Don’t forget to reset any map settings that were changed, such as camera tracking, location marker, and map padding.
     */
    private fun stopNavigation() {
        navigationFragment.stopNavigation()
        mapFragment.currentLocationButton.visibilityPolicy =
            VisibilityPolicy.InvisibleWhenRecentered
        tomTomMap.removeCameraChangeListener(cameraChangeListener)
        tomTomMap.cameraTrackingMode = CameraTrackingMode.None
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Pointer))
        resetMapPadding()
        navigationFragment.removeNavigationListener(navigationListener)
        tomTomNavigation.removeProgressUpdatedListener(progressUpdatedListener)
        tomTomNavigation.removeActiveRouteChangedListener(activeRouteChangedListener)
        clearMap()
        initLocationProvider()
        enableUserLocation()
    }

    /**
     * Set the bottom padding on the map. The padding sets a safe area of the MapView in which user interaction is not received. It is used to uncover the chevron in the navigation panel.
     */
    private fun setMapNavigationPadding() {
        // This paddingBottom provides 920 which makes the chevron go off the screen
        // val paddingBottom = resources.getDimensionPixelOffset(R.dimen.map_padding_bottom)
        val padding = Padding(0, 0, 0, CHEVRON_PADDING)
        setPadding(padding)
    }

    private fun setPadding(padding: Padding) {
        val scale: Float = context.resources.displayMetrics.density
        val paddingInPixels = Padding(
            top = (padding.top * scale).toInt(),
            left = (padding.left * scale).toInt(),
            right = (padding.right * scale).toInt(),
            bottom = (padding.bottom * scale).toInt()
        )
        tomTomMap.setPadding(paddingInPixels)
    }

    private fun resetMapPadding() {
        tomTomMap.setPadding(Padding(0, 0, 0, 0))
    }

    /**
     * Once navigation is started, the camera is set to follow the user position, and the location indicator is changed to a chevron.
     * To match raw location updates to the routes, use MapMatchedLocationProvider and set it to the TomTomMap.
     */
    private fun setMapMatchedLocationProvider() {
        val mapMatchedLocationProvider = MapMatchedLocationProvider(tomTomNavigation)
        tomTomMap.setLocationProvider(mapMatchedLocationProvider)
        mapMatchedLocationProvider.enable()
    }

    /**
     *
     * The method removes all polygons, circles, routes, and markers that were previously added to the map.
     */
    private fun clearMap() {
        tomTomMap.clear()
    }

//    private fun requestLocationPermission() {
//        locationPermissionRequest.launch(
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//        )
//    }

    private val cameraChangeListener by lazy {
        CameraChangeListener {
            val cameraTrackingMode = tomTomMap.cameraTrackingMode
            if (cameraTrackingMode == CameraTrackingMode.FollowRouteDirection) {
                navigationFragment.navigationView.showSpeedView()
            } else {
                navigationFragment.navigationView.hideSpeedView()
            }
        }
    }

//    private val locationPermissionRequest = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
//            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
//        ) {
//            showUserLocation()
//        } else {
//            Timber.w(context.getString(R.string.location_permission_denied))
//        }
//    }

//    /**
//     * Method to verify permissions:
//     * - [Manifest.permission.ACCESS_FINE_LOCATION]
//     * - [Manifest.permission.ACCESS_COARSE_LOCATION]
//     * - [Manifest.permission.FOREGROUND_SERVICE_LOCATION]
//     */
//    private fun areLocationPermissionsGranted(): Boolean {
//        val fineLocationGranted = ContextCompat.checkSelfPermission(
//            context, Manifest.permission.ACCESS_FINE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED
//
//        val coarseLocationGranted = ContextCompat.checkSelfPermission(
//            context, Manifest.permission.ACCESS_COARSE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED
//
//        val isForegroundServiceLocationRequired =
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
//        val foregroundServiceLocationGranted = if (isForegroundServiceLocationRequired) {
//            ContextCompat.checkSelfPermission(
//                context, Manifest.permission.FOREGROUND_SERVICE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        } else true // Assume granted for lower versions
//
//        return fineLocationGranted && coarseLocationGranted && foregroundServiceLocationGranted
//    }


    override fun onCleared() {
        super.onCleared()
        tomTomMap.setLocationProvider(null)
        tomTomNavigation.close()
        navigationTileStore.close()
        locationProvider.close()
    }

    companion object {
        private const val ZOOM_TO_ROUTE_PADDING = 100
        private const val CHEVRON_PADDING = 263
    }
}
