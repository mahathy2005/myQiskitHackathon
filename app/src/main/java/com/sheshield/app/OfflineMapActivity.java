package com.sheshield.app;

// ============================================================
// ANDROID IMPORTS
// ============================================================

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executors;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// ============================================================
// ANDROIDX
// ============================================================

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// ============================================================
// MATERIAL
// ============================================================

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// ============================================================
// FIREBASE
// ============================================================

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

// ============================================================
// GRAPH HOPPER OFFLINE ROUTING
// ============================================================

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Parameters;
import java.util.List;

// ============================================================
// OSMDROID
// ============================================================

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.events.MapEventsReceiver;

// ============================================================
// JSON
// ============================================================

import org.json.JSONArray;
import org.json.JSONObject;

// ============================================================
// JAVA
// ============================================================

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


// ============================================================
// ACTIVITY
// ============================================================

public class OfflineMapActivity extends BaseActivity
        implements MapEventsReceiver {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private static final String PREF_NAME =
            "SheShieldOfflineMap";

    private static final String KEY_SAVED_ROUTE =
            "saved_route";

    private static final String KEY_SAVED_DESTINATION =
            "saved_destination";

    private static final String KEY_SAVED_DESTINATION_NAME =
            "saved_destination_name";

    private static final String KEY_SAVED_TRANSPORT_MODE =
            "saved_transport_mode";

    private static final double SOS_RADIUS_METERS = 120.0;

    // Development-only address of the SheShield Route Recommendation API.
    // Update this if the laptop's Wi-Fi IPv4 address changes.
    private static final String SHE_SHIELD_ROUTE_API =
            "http://192.168.31.171:8000/rank-routes";


    // =========================================================
    // MAP
    // =========================================================

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;

    private MapsForgeTileProvider offlineMapTileProvider;
    private SimpleRegisterReceiver offlineMapReceiver;

    private java.util.concurrent.ExecutorService offlineMapExecutor;

    // Direct GNSS location used for offline navigation.
    // GNSS works from satellite signals and does not require mobile data.
    private LocationManager locationManager;
    private Location latestGnssLocation;
    private boolean gnssStarted = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback fusedLocationCallback;


    private final LocationListener gnssLocationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (location == null) {
                        return;
                    }

                    // Ignore obviously invalid coordinates.
                    if (location.getLatitude() < -90
                            || location.getLatitude() > 90
                            || location.getLongitude() < -180
                            || location.getLongitude() > 180) {
                        return;
                    }

                    latestGnssLocation = new Location(location);

                    if (navigationStarted && mapView != null) {
                        mapView.invalidate();
                    }
                }

                @Override
                public void onProviderEnabled(@NonNull String provider) {
                    // GNSS became available again.
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    // The navigation code keeps the last valid GNSS fix.
                }
            };

    private Marker destinationMarker;

    private Polyline routeLine;


    // =========================================================
    // SEARCH
    // =========================================================

    private AutoCompleteTextView etDestination;

    private TextView tvNetworkStatus;

    private TextView tvRouteStatus;

    private TextView tvDestinationName;

    private TextView tvDestinationAddress;

    private TextView tvTravelInfo;

    private TextView tvTravelMode;

    private TextView tvRouteType;

    private TextView tvNavigationTitle;

    private TextView tvNavigationInfo;


    private SearchSuggestionAdapter suggestionAdapter;

    // Prevents older network responses from replacing newer searches.
    private int searchRequestId = 0;


    /*
     * Online Photon suggestions.
     */
    private final ArrayList<PhotonPlace> photonSuggestions =
            new ArrayList<>();


    /*
     * Local offline places.
     */
    private final ArrayList<LocalPlace> offlinePlaces =
            new ArrayList<>();


    /*
     * Search handler.
     *
     * This prevents a request from being sent for
     * every single character typed.
     */
    private final Handler searchHandler =
            new Handler(Looper.getMainLooper());

    // Runs Photon requests away from the UI thread.
    private final java.util.concurrent.ExecutorService searchExecutor =
            Executors.newSingleThreadExecutor();


    private Runnable searchRunnable;


    // =========================================================
    // DESTINATION
    // =========================================================

    private GeoPoint selectedDestination;

    private String selectedDestinationName = "";

    private String selectedTransportMode = "car";


    // =========================================================
    // NAVIGATION
    // =========================================================

    private boolean navigationStarted = false;

    private boolean suppressSearchWatcher = false;

    private double selectedRouteDistanceMeters = 0;

    private double selectedRouteDurationSeconds = 0;

    // Full route geometry currently being used for navigation.
    // This lets us calculate remaining distance along the route
    // instead of using a straight-line distance.
    private final ArrayList<GeoPoint> currentRoutePoints =
            new ArrayList<>();

    // Stable navigation progress. Normal GPS noise must not make the
    // remaining distance/ETA move backwards while the user is walking.
    private double lastNavigationRemainingMeters = -1.0;
    private int lastNavigationSegmentIndex = 0;

    private Runnable navigationUpdater;


    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());


    // =========================================================
    // TRUE OFFLINE ROUTING
    // =========================================================

    /*
     * GraphHopper is used only for the offline road graph.
     * The graph itself must already be generated from OpenStreetMap
     * data and placed under app/src/main/assets/graphhopper/.
     *
     * GNSS supplies the live start point; GraphHopper supplies the
     * road route. This means the route does not need internet access.
     */
    private GraphHopper offlineHopper;

    private final java.util.concurrent.ExecutorService
            offlineRoutingExecutor =
            Executors.newSingleThreadExecutor();

    private volatile boolean offlineRouterReady = false;

    private volatile boolean offlineRouterFailed = false;

    private volatile String offlineRouterError =
            "Offline road data is not installed.";

    private static final String OFFLINE_GRAPH_ASSET_FOLDER =
            "graphhopper";

    private static final String OFFLINE_GRAPH_FOLDER_NAME =
            "graphhopper";


    // =========================================================
    // BUTTONS / UI
    // =========================================================

    private Button btnSafestRoute;

    private Button btnFastestRoute;

    private Button btnStartNavigation;

    private Button btnStopNavigation;


    private MaterialButton btnCar;

    private MaterialButton btnWalk;

    private MaterialButton btnBike;

    private MaterialButton btnTransit;


    private FloatingActionButton fabMyLocation;

    private MaterialButton btnTriggerSOS;


    private MaterialCardView destinationPanel;

    private LinearLayout layoutRouteOptions;

    private LinearLayout layoutDirections;

    private MaterialCardView navigationPanel;


    // =========================================================
    // FIRESTORE INCIDENT DATA
    // =========================================================

    private FirebaseFirestore firestore;

    private FirebaseAuth firebaseAuth;

    private ListenerRegistration incidentListener;

    private final HashMap<String, IncidentOverlay> incidentOverlays =
            new HashMap<>();


    // =========================================================
    // ACTIVITY
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .load(
                        this,
                        getSharedPreferences(
                                "osmdroid",
                                MODE_PRIVATE
                        )
                );

        setContentView(
                R.layout.fragment_offline_map
        );

        findViews();

        setupMap();

        loadOfflinePlaces();

        setupLocation();

        setupSearch();

        setupButtons();

        setupTransportModes();

        setupFirebaseIncidents();

        updateNetworkStatus();

        loadSavedRoute();

        handleSafeZoneIntent();

// Load the local road graph in the background.
// It does not use the internet.
        prepareOfflineRoutingEngine();
    }

    private void handleSafeZoneIntent() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        // Check whether SafeZonesActivity opened this page
        if (!intent.getBooleanExtra(
                "open_safe_zone",
                false
        )) {
            return;
        }

        // ---------------------------------------------------------
        // GET SAFE ZONE COORDINATES
        // ---------------------------------------------------------

        double latitude =
                intent.getDoubleExtra(
                        "safe_zone_latitude",
                        Double.NaN
                );

        double longitude =
                intent.getDoubleExtra(
                        "safe_zone_longitude",
                        Double.NaN
                );

        // ---------------------------------------------------------
        // CHECK COORDINATES
        // ---------------------------------------------------------

        if (
                Double.isNaN(latitude)
                        || Double.isNaN(longitude)
                        || latitude < -90
                        || latitude > 90
                        || longitude < -180
                        || longitude > 180
        ) {

            Toast.makeText(
                    OfflineMapActivity.this,
                    "Invalid safe-zone location.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // GET SAFE ZONE NAME
        // ---------------------------------------------------------

        String name =
                intent.getStringExtra(
                        "safe_zone_name"
                );

        if (
                name == null
                        || name.trim().isEmpty()
        ) {

            name = "Safe Place";
        }

        // ---------------------------------------------------------
        // GET SAFE ZONE ADDRESS
        // ---------------------------------------------------------

        String address =
                intent.getStringExtra(
                        "safe_zone_address"
                );

        final String destinationAddress =
                address == null
                        ? ""
                        : address;

        // ---------------------------------------------------------
        // SET SELECTED DESTINATION
        // ---------------------------------------------------------

        selectedDestination =
                new GeoPoint(
                        latitude,
                        longitude
                );

        selectedDestinationName =
                name.trim();

        // ---------------------------------------------------------
        // SHOW IT ON THE OFFLINE MAP
        // ---------------------------------------------------------

        if (mapView != null) {

            mapView.post(
                    new Runnable() {

                        @Override
                        public void run() {

                            showDestinationOnMap();

                            showDestinationPanel(
                                    destinationAddress
                            );

                            if (tvRouteStatus != null) {

                                tvRouteStatus.setText(
                                        "Safe place selected."
                                );
                            }
                        }
                    }
            );
        }

        // Prevent this same intent from being processed again
        intent.removeExtra(
                "open_safe_zone"
        );
    }

    // =========================================================
    // FIND VIEWS
    // =========================================================

    private void findViews() {

        mapView =
                findViewById(R.id.map);

        etDestination =
                findViewById(R.id.etDestination);

        tvNetworkStatus =
                findViewById(R.id.tvNetworkStatus);

        tvRouteStatus =
                findViewById(R.id.tvRouteStatus);

        destinationPanel =
                findViewById(R.id.destinationPanel);

        tvDestinationName =
                findViewById(R.id.tvDestinationName);

        tvDestinationAddress =
                findViewById(R.id.tvDestinationAddress);

        btnCar =
                findViewById(R.id.btnCar);

        btnWalk =
                findViewById(R.id.btnWalk);

        btnBike =
                findViewById(R.id.btnBike);

        btnTransit =
                findViewById(R.id.btnTransit);

        layoutRouteOptions =
                findViewById(R.id.layoutRouteOptions);

        btnSafestRoute =
                findViewById(R.id.btnSafestRoute);

        btnFastestRoute =
                findViewById(R.id.btnFastestRoute);

        layoutDirections =
                findViewById(R.id.layoutDirections);

        tvTravelInfo =
                findViewById(R.id.tvTravelInfo);

        tvTravelMode =
                findViewById(R.id.tvTravelMode);

        tvRouteType =
                findViewById(R.id.tvRouteType);

        btnStartNavigation =
                findViewById(R.id.btnStartNavigation);

        navigationPanel =
                findViewById(R.id.navigationPanel);

        tvNavigationTitle =
                findViewById(R.id.tvNavigationTitle);

        tvNavigationInfo =
                findViewById(R.id.tvNavigationInfo);

        btnStopNavigation =
                findViewById(R.id.btnStopNavigation);

        fabMyLocation =
                findViewById(R.id.fabMyLocation);

        btnTriggerSOS =
                findViewById(R.id.btnTriggerSOS);
    }


    private File prepareOfflineMapFile() {

        File mapDir =
                new File(
                        getFilesDir(),
                        "mapsforge"
                );

        if (!mapDir.exists()
                && !mapDir.mkdirs()) {

            Log.e(
                    "OfflineMap",
                    "Could not create mapsforge directory"
            );

            return null;
        }

        File mapFile =
                new File(
                        mapDir,
                        "southern-zone.map"
                );

        // ---------------------------------------------------------
        // MAP ALREADY EXISTS
        // ---------------------------------------------------------

        if (mapFile.exists()
                && mapFile.length() > 0) {

            Log.d(
                    "OfflineMap",
                    "Offline map already exists: "
                            + mapFile.length()
                            + " bytes"
            );

            return mapFile;
        }


        // ---------------------------------------------------------
        // MAP DOES NOT EXIST YET
        // ---------------------------------------------------------

        if (offlineMapExecutor == null) {

            offlineMapExecutor =
                    java.util.concurrent.Executors
                            .newSingleThreadExecutor();
        }


        offlineMapExecutor.execute(() -> {

            File temporaryFile =
                    new File(
                            mapDir,
                            "southern-zone.map.tmp"
                    );

            try {

                Log.d(
                        "OfflineMap",
                        "Starting offline map copy..."
                );

                InputStream inputStream =
                        getAssets().open(
                                "mapsforge/southern-zone.map"
                        );

                FileOutputStream outputStream =
                        new FileOutputStream(
                                temporaryFile
                        );

                byte[] buffer =
                        new byte[1024 * 1024];

                int length;

                while (
                        (length =
                                inputStream.read(buffer))
                                != -1
                ) {

                    outputStream.write(
                            buffer,
                            0,
                            length
                    );
                }

                outputStream.flush();

                outputStream.close();
                inputStream.close();


                // -------------------------------------------------
                // REPLACE OLD/INCOMPLETE FILE
                // -------------------------------------------------

                if (mapFile.exists()) {
                    mapFile.delete();
                }

                if (!temporaryFile.renameTo(mapFile)) {

                    throw new Exception(
                            "Could not rename temporary map file"
                    );
                }


                Log.d(
                        "OfflineMap",
                        "Offline map copied successfully: "
                                + mapFile.length()
                                + " bytes"
                );


                // -------------------------------------------------
                // MAP IS READY
                // -------------------------------------------------

                runOnUiThread(() -> {

                    if (isFinishing()
                            || isDestroyed()) {
                        return;
                    }

                    Log.d(
                            "OfflineMap",
                            "Offline map is ready"
                    );

                    setupMap();

                });


            } catch (Exception e) {

                Log.e(
                        "OfflineMap",
                        "Failed to copy offline map",
                        e
                );

                if (temporaryFile.exists()) {
                    temporaryFile.delete();
                }

                runOnUiThread(() -> {

                    if (isFinishing()
                            || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            "Offline map could not be loaded",
                            Toast.LENGTH_LONG
                    ).show();

                });
            }
        });


        // ---------------------------------------------------------
        // IMPORTANT:
        // Do NOT block the UI while the 458 MB file is copying.
        // ---------------------------------------------------------

        return null;
    }

    // =========================================================
    // MAP
    // =========================================================

    private void setupMap() {

        // =========================================================
        // HYBRID OSMDROID MAP
        //
        // Online  -> download normal MAPNIK tiles and keep them in
        //             the OSMDroid tile cache.
        // Offline -> use the same cached tiles; no Mapsforge map is
        //             required for the map display.
        // =========================================================

        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(4.0);
        mapView.setMaxZoomLevel(20.0);

        // Normal OpenStreetMap tiles. OSMDroid stores downloaded tiles
        // in its local tile cache.
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Online: tiles can be downloaded and cached.
        // Offline: network access is disabled, so cached tiles are used.
        mapView.setUseDataConnection(isNetworkAvailable());

        // ---------------------------------------------------------
        // INITIAL MAP POSITION
        // ---------------------------------------------------------

        IMapController controller = mapView.getController();

        GeoPoint initialLocation = getBestAvailableLocation();

        if (initialLocation == null) {
            initialLocation = new GeoPoint(12.9716, 77.5946);
        }

        controller.setCenter(initialLocation);
        controller.setZoom(15.0);

        // ---------------------------------------------------------
        // MAP EVENTS
        // ---------------------------------------------------------

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(this);
        mapView.getOverlays().add(eventsOverlay);

        Log.d(
                "HybridMap",
                "OSMDroid hybrid map enabled. Network="
                        + isNetworkAvailable()
        );

        mapView.invalidate();
    }


    // =========================================================
    // LOCATION
    // =========================================================

    private void setupLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        try {

            // ---------------------------------------------------------
            // OSMDROID LOCATION OVERLAY
            // ---------------------------------------------------------

            GpsMyLocationProvider provider =
                    new GpsMyLocationProvider(this);

            myLocationOverlay =
                    new MyLocationNewOverlay(
                            provider,
                            mapView
                    );

            myLocationOverlay.enableMyLocation();

            mapView.getOverlays().add(
                    myLocationOverlay
            );

            // ---------------------------------------------------------
            // GOOGLE FUSED LOCATION
            // ---------------------------------------------------------

            fusedLocationClient =
                    LocationServices
                            .getFusedLocationProviderClient(this);

            LocationRequest locationRequest =
                    new LocationRequest.Builder(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            1000L
                    )
                            .setMinUpdateIntervalMillis(500L)
                            .setMinUpdateDistanceMeters(1.0f)
                            .build();

            fusedLocationCallback =
                    new LocationCallback() {

                        @Override
                        public void onLocationResult(
                                @NonNull LocationResult locationResult
                        ) {

                            Location location =
                                    locationResult
                                            .getLastLocation();

                            if (location == null) {
                                return;
                            }

                            if (location.getLatitude() < -90
                                    || location.getLatitude() > 90
                                    || location.getLongitude() < -180
                                    || location.getLongitude() > 180) {

                                return;
                            }

                            latestGnssLocation =
                                    new Location(location);

                            gnssStarted = true;

                            if (navigationStarted
                                    && mapView != null) {

                                mapView.invalidate();
                            }
                        }
                    };

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    fusedLocationCallback,
                    Looper.getMainLooper()
            );

            // ---------------------------------------------------------
            // ALSO START DIRECT GNSS
            // ---------------------------------------------------------

            startGnssLocation();

        } catch (SecurityException e) {

            Log.e(
                    "Location",
                    "Location permission error",
                    e
            );

        } catch (Exception e) {

            Log.e(
                    "Location",
                    "Failed to initialize location",
                    e
            );
        }
    }


    private void enableLocation() {

        // Keep the OSMDroid blue location marker.
        if (myLocationOverlay != null) {
            mapView.getOverlays().remove(myLocationOverlay);
        }

        GpsMyLocationProvider provider =
                new GpsMyLocationProvider(this);

        myLocationOverlay =
                new MyLocationNewOverlay(
                        provider,
                        mapView
                );

        myLocationOverlay.enableMyLocation();

        mapView.getOverlays().add(myLocationOverlay);

        // Also receive the raw GPS/GNSS fixes directly.
        startGnssLocation();

        mapView.invalidate();
    }


    private void startGnssLocation() {

        if (gnssStarted) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return;
        }

        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(
                        this,
                        "Please turn on Location/GPS for offline navigation.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            // GPS_PROVIDER receives satellite/GNSS fixes. No internet is needed.
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1.0f,
                    gnssLocationListener,
                    Looper.getMainLooper()
            );

            Location lastKnown =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastKnown != null) {
                latestGnssLocation = new Location(lastKnown);
            }

            gnssStarted = true;

        } catch (SecurityException e) {
            gnssStarted = false;
        } catch (Exception e) {
            gnssStarted = false;
        }
    }


    private void stopGnssLocation() {

        // ---------------------------------------------------------
        // STOP DIRECT GNSS
        // ---------------------------------------------------------

        if (locationManager != null
                && gnssStarted) {

            try {

                locationManager.removeUpdates(
                        gnssLocationListener
                );

            } catch (SecurityException e) {

                Log.e(
                        "Location",
                        "Failed to stop GNSS",
                        e
                );
            }
        }

        gnssStarted = false;

        // ---------------------------------------------------------
        // STOP GOOGLE FUSED LOCATION
        // ---------------------------------------------------------

        if (fusedLocationClient != null
                && fusedLocationCallback != null) {

            try {

                fusedLocationClient.removeLocationUpdates(
                        fusedLocationCallback
                );

            } catch (Exception e) {

                Log.e(
                        "Location",
                        "Failed to stop fused location",
                        e
                );
            }
        }

        // ---------------------------------------------------------
        // STOP OSMDROID LOCATION OVERLAY
        // ---------------------------------------------------------

        if (myLocationOverlay != null) {

            try {

                myLocationOverlay.disableMyLocation();

            } catch (Exception ignored) {
            }
        }
    }


    private GeoPoint getBestAvailableLocation() {

        // Prefer the direct GNSS fix for navigation.
        if (latestGnssLocation != null) {
            return new GeoPoint(
                    latestGnssLocation.getLatitude(),
                    latestGnssLocation.getLongitude()
            );
        }

        // Fall back to OSMDroid if a direct GNSS fix has not arrived yet.
        if (myLocationOverlay != null) {
            return myLocationOverlay.getMyLocation();
        }

        return null;
    }


    private void goToMyLocation() {

        GeoPoint myLocation =
                getBestAvailableLocation();

        if (myLocation == null) {

            Toast.makeText(
                    this,
                    "Waiting for your GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        mapView.getController()
                .animateTo(myLocation);

        mapView.getController()
                .setZoom(17.0);
    }


    // =========================================================
    // SEARCH
    // PHOTON + OFFLINE SEARCH
    // =========================================================

    private void setupSearch() {

        // =========================================================
        // SEARCH BAR - LIVE SUGGESTIONS
        // =========================================================
        // The search bar is intentionally handled here instead of
        // waiting for the Search/keyboard button.
        //
        // Type:
        //     p      -> suggestions begin loading
        //     pr     -> refreshed suggestions
        //     pres   -> refreshed suggestions
        //
        // Online  : Photon suggestions
        // Offline : local safe/offline places
        //
        // A newer request always invalidates the older request.
        // =========================================================

        suggestionAdapter =
                new SearchSuggestionAdapter(this);

        etDestination.setAdapter(
                suggestionAdapter
        );

        // Start after one character.
        etDestination.setThreshold(1);

        etDestination.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        etDestination.setSingleLine(true);

        etDestination.setImeOptions(
                EditorInfo.IME_ACTION_SEARCH
        );

        // Give the suggestion list enough room to be useful.
        etDestination.setDropDownVerticalOffset(
                dpToPx(6)
        );

        etDestination.setDropDownHeight(
                dpToPx(330)
        );

        etDestination.setDropDownWidth(
                etDestination.getWidth() > 0
                        ? etDestination.getWidth()
                        : -1
        );

        // ---------------------------------------------------------
        // USER TAPS A SUGGESTION
        // ---------------------------------------------------------
        etDestination.setOnItemClickListener(
                (parent, view, position, id) -> {

                    PhotonPlace place =
                            suggestionAdapter.getItem(position);

                    if (place != null) {
                        selectPhotonPlace(place);
                    }
                }
        );

        // ---------------------------------------------------------
        // LIVE SEARCH WHILE TYPING
        // ---------------------------------------------------------
        etDestination.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                        // Nothing needed here.
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {

                        if (suppressSearchWatcher) {
                            return;
                        }

                        String query =
                                text == null
                                        ? ""
                                        : text.toString().trim();

                        // Every keystroke makes previous results stale.
                        final int requestId =
                                ++searchRequestId;

                        searchHandler.removeCallbacksAndMessages(
                                null
                        );

                        // Empty search = clear suggestions.
                        if (query.isEmpty()) {

                            photonSuggestions.clear();
                            suggestionAdapter.clear();
                            suggestionAdapter.notifyDataSetChanged();
                            etDestination.dismissDropDown();
                            return;
                        }

                        // Wait only a short moment so fast typing does not
                        // create a request for every intermediate character.
                        searchHandler.postDelayed(
                                () -> {

                                    if (requestId != searchRequestId) {
                                        return;
                                    }

                                    if (!etDestination.hasFocus()) {
                                        return;
                                    }

                                    if (isNetworkAvailable()) {

                                        loadPhotonSuggestions(
                                                query,
                                                requestId
                                        );

                                    } else {

                                        loadOfflineDropdownSuggestions(
                                                query
                                        );
                                    }

                                },
                                250
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        // Nothing needed here.
                    }
                }
        );

        // ---------------------------------------------------------
        // WHEN USER OPENS THE SEARCH BAR AGAIN
        // ---------------------------------------------------------
        // If text is already present, immediately repopulate the
        // dropdown instead of forcing the user to type another key.
        etDestination.setOnFocusChangeListener(
                (view, hasFocus) -> {

                    if (!hasFocus) {
                        return;
                    }

                    String query =
                            etDestination.getText()
                                    .toString()
                                    .trim();

                    if (query.isEmpty()) {
                        return;
                    }

                    final int requestId =
                            ++searchRequestId;

                    searchHandler.removeCallbacksAndMessages(null);

                    searchHandler.postDelayed(
                            () -> {

                                if (requestId != searchRequestId) {
                                    return;
                                }

                                if (isNetworkAvailable()) {

                                    loadPhotonSuggestions(
                                            query,
                                            requestId
                                    );

                                } else {

                                    loadOfflineDropdownSuggestions(
                                            query
                                    );
                                }

                            },
                            100
                    );
                }
        );

        // ---------------------------------------------------------
        // SEARCH BUTTON / KEYBOARD
        // ---------------------------------------------------------
        // Keep the existing full-search behaviour as a fallback.
        etDestination.setOnEditorActionListener(
                (v, actionId, event) -> {

                    boolean searchPressed =
                            actionId == EditorInfo.IME_ACTION_SEARCH
                                    ||
                                    actionId == EditorInfo.IME_ACTION_DONE;

                    if (
                            !searchPressed
                                    &&
                                    event != null
                                    &&
                                    event.getKeyCode() ==
                                            android.view.KeyEvent.KEYCODE_ENTER
                    ) {
                        searchPressed = true;
                    }

                    if (!searchPressed) {
                        return false;
                    }

                    String query =
                            etDestination.getText()
                                    .toString()
                                    .trim();

                    if (query.isEmpty()) {
                        return true;
                    }

                    searchHandler.removeCallbacksAndMessages(
                            null
                    );

                    searchRequestId++;

                    etDestination.dismissDropDown();

                    if (isNetworkAvailable()) {

                        searchPhoton(query);

                    } else {

                        searchOfflinePlaces(query);
                    }

                    return true;
                }
        );
    }

    private void loadPhotonSuggestions(
            String query,
            int requestId
    ) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String encodedQuery =
                        URLEncoder.encode(
                                query,
                                StandardCharsets.UTF_8.toString()
                        );


                StringBuilder urlBuilder =
                        new StringBuilder(
                                "https://photon.komoot.io/api/?q="
                        );

                urlBuilder.append(
                        encodedQuery
                );

                /*
                 * Maximum 7 suggestions.
                 */
                urlBuilder.append(
                        "&limit=10"
                );

                /*
                 * English names where available.
                 */
                urlBuilder.append(
                        "&lang=en"
                );


                // =================================================
                // CURRENT LOCATION BIAS
                // =================================================

                GeoPoint currentLocation =
                        getBestAvailableLocation();

                if (currentLocation != null) {

                    urlBuilder.append(
                            "&lat="
                    ).append(
                            currentLocation.getLatitude()
                    );

                    urlBuilder.append(
                            "&lon="
                    ).append(
                            currentLocation.getLongitude()
                    );

                    /*
                     * Zoom level around the user.
                     */
                    urlBuilder.append(
                            "&zoom=13"
                    );

                    /*
                     * Encourage nearby results.
                     */
                    urlBuilder.append(
                            "&location_bias_scale=0.6"
                    );
                }


                URL url =
                        new URL(
                                urlBuilder.toString()
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        7000
                );

                connection.setReadTimeout(
                        7000
                );

                connection.setUseCaches(
                        false
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "SheShield Android App"
                );


                int responseCode =
                        connection.getResponseCode();


                if (responseCode != 200) {

                    runOnUiThread(() ->
                            loadOfflineDropdownSuggestions(
                                    query
                            )
                    );

                    return;
                }


                InputStream inputStream =
                        connection.getInputStream();


                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream
                                )
                        );


                StringBuilder response =
                        new StringBuilder();


                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    response.append(line);
                }


                reader.close();

                inputStream.close();


                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );


                JSONArray features =
                        json.optJSONArray(
                                "features"
                        );


                final ArrayList<PhotonPlace> results =
                        new ArrayList<>();


                if (features != null) {

                    for (
                            int i = 0;
                            i < features.length();
                            i++
                    ) {

                        JSONObject feature =
                                features.optJSONObject(
                                        i
                                );


                        if (feature == null) {
                            continue;
                        }


                        JSONObject properties =
                                feature.optJSONObject(
                                        "properties"
                                );


                        JSONObject geometry =
                                feature.optJSONObject(
                                        "geometry"
                                );


                        if (properties == null ||
                                geometry == null) {

                            continue;
                        }


                        JSONArray coordinates =
                                geometry.optJSONArray(
                                        "coordinates"
                                );


                        if (coordinates == null ||
                                coordinates.length() < 2) {

                            continue;
                        }


                        double longitude =
                                coordinates.optDouble(
                                        0
                                );


                        double latitude =
                                coordinates.optDouble(
                                        1
                                );


                        if (Double.isNaN(latitude) ||
                                Double.isNaN(longitude)) {

                            continue;
                        }


                        String name =
                                properties.optString(
                                        "name",
                                        ""
                                ).trim();


                        String street =
                                properties.optString(
                                        "street",
                                        ""
                                ).trim();


                        String housenumber =
                                properties.optString(
                                        "housenumber",
                                        ""
                                ).trim();


                        String locality =
                                properties.optString(
                                        "locality",
                                        ""
                                ).trim();


                        String district =
                                properties.optString(
                                        "district",
                                        ""
                                ).trim();


                        String city =
                                properties.optString(
                                        "city",
                                        ""
                                ).trim();


                        String state =
                                properties.optString(
                                        "state",
                                        ""
                                ).trim();


                        String country =
                                properties.optString(
                                        "country",
                                        ""
                                ).trim();


                        String address =
                                buildPhotonAddress(
                                        street,
                                        housenumber,
                                        locality,
                                        district,
                                        city,
                                        state,
                                        country
                                );


                        /*
                         * Some Photon results don't have
                         * a name.
                         */
                        if (name.isEmpty()) {

                            if (!street.isEmpty()) {

                                name = street;

                            } else if (
                                    !locality.isEmpty()
                            ) {

                                name = locality;

                            } else if (
                                    !city.isEmpty()
                            ) {

                                name = city;

                            } else {

                                name =
                                        "Selected location";
                            }
                        }


                        /*
                         * Avoid duplicate suggestions.
                         */
                        boolean duplicate = false;

                        for (
                                PhotonPlace existing :
                                results
                        ) {

                            if (
                                    existing.name
                                            .equalsIgnoreCase(
                                                    name
                                            )
                                            &&
                                            Math.abs(
                                                    existing.latitude
                                                            - latitude
                                            ) < 0.0001
                                            &&
                                            Math.abs(
                                                    existing.longitude
                                                            - longitude
                                            ) < 0.0001
                            ) {

                                duplicate = true;

                                break;
                            }
                        }


                        if (duplicate) {
                            continue;
                        }


                        results.add(
                                new PhotonPlace(
                                        name,
                                        address,
                                        latitude,
                                        longitude
                                )
                        );


                        if (results.size() >= 7) {
                            break;
                        }
                    }
                }


                runOnUiThread(() -> {

                    if (requestId != searchRequestId) {
                        return;
                    }

                    /*
                     * If there are no online results,
                     * fall back to offline places.
                     */
                    if (results.isEmpty()) {

                        loadOfflineDropdownSuggestions(
                                query
                        );

                        return;
                    }


                    photonSuggestions.clear();

                    photonSuggestions.addAll(
                            results
                    );


                    suggestionAdapter.clear();
                    suggestionAdapter.addAll(
                            photonSuggestions
                    );
                    suggestionAdapter.notifyDataSetChanged();


                    if (
                            !photonSuggestions.isEmpty()
                                    &&
                                    etDestination.hasFocus()
                    ) {

                        etDestination.showDropDown();
                    }
                });


            } catch (Exception e) {

                runOnUiThread(() ->
                        loadOfflineDropdownSuggestions(
                                query
                        )
                );


            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    // =========================================================
    // PHOTON ADDRESS FORMATTER
    // =========================================================

    private String buildPhotonAddress(
            String street,
            String housenumber,
            String locality,
            String district,
            String city,
            String state,
            String country
    ) {

        ArrayList<String> parts =
                new ArrayList<>();


        if (!street.isEmpty()) {

            if (!housenumber.isEmpty()) {

                parts.add(
                        street
                                + " "
                                + housenumber
                );

            } else {

                parts.add(
                        street
                );
            }
        }


        if (
                !locality.isEmpty()
                        &&
                        !parts.contains(locality)
        ) {

            parts.add(
                    locality
            );
        }


        if (
                !district.isEmpty()
                        &&
                        !parts.contains(district)
        ) {

            parts.add(
                    district
            );
        }


        if (
                !city.isEmpty()
                        &&
                        !parts.contains(city)
        ) {

            parts.add(
                    city
            );
        }


        if (
                !state.isEmpty()
                        &&
                        !parts.contains(state)
        ) {

            parts.add(
                    state
            );
        }


        if (
                !country.isEmpty()
                        &&
                        !parts.contains(country)
        ) {

            parts.add(
                    country
            );
        }


        StringBuilder result =
                new StringBuilder();


        for (String part : parts) {

            if (result.length() > 0) {

                result.append(
                        ", "
                );
            }

            result.append(
                    part
            );
        }


        return result.toString();
    }


    // =========================================================
    // SELECT PHOTON PLACE
    // =========================================================

    private void selectPhotonPlace(
            PhotonPlace place
    ) {

        if (place == null) {
            return;
        }

        if (
                Double.isNaN(place.latitude)
                        ||
                        Double.isNaN(place.longitude)
                        ||
                        place.latitude < -90
                        ||
                        place.latitude > 90
                        ||
                        place.longitude < -180
                        ||
                        place.longitude > 180
        ) {
            Toast.makeText(
                    this,
                    "This location could not be identified correctly.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        selectedDestination =
                new GeoPoint(
                        place.latitude,
                        place.longitude
                );

        selectedDestinationName =
                place.name == null ||
                        place.name.trim().isEmpty()
                        ? "Selected location"
                        : place.name.trim();

        suppressSearchWatcher = true;

        /*
         * Keep the place name in the search bar.
         * The complete address is shown in the destination card.
         */
        etDestination.setText(
                selectedDestinationName
        );

        etDestination.setSelection(
                etDestination.length()
        );

        suppressSearchWatcher = false;

        photonSuggestions.clear();
        suggestionAdapter.clear();
        etDestination.dismissDropDown();

        showDestinationOnMap();

        showDestinationPanel(
                place.address
        );

        tvRouteStatus.setText(
                "Destination identified."
        );
    }


    // =========================================================
    // PHOTON SEARCH WHEN USER PRESSES SEARCH
    // =========================================================

    private void searchPhoton(String query) {

        if (query == null || query.trim().isEmpty()) {
            return;
        }

        final String cleanQuery = query.trim();
        final int requestId = ++searchRequestId;

        searchExecutor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String encodedQuery =
                        URLEncoder.encode(
                                cleanQuery,
                                StandardCharsets.UTF_8.toString()
                        );

                String urlString =
                        "https://photon.komoot.io/api/?q="
                                + encodedQuery
                                + "&limit=1";

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "SheShield Android App"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    runOnUiThread(() -> {

                        if (requestId != searchRequestId) {
                            return;
                        }

                        Toast.makeText(
                                this,
                                "Search error. Please check connection.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });

                    return;
                }

                InputStream inputStream =
                        connection.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine()) != null
                ) {
                    response.append(line);
                }

                reader.close();
                inputStream.close();

                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );

                JSONArray features =
                        json.optJSONArray(
                                "features"
                        );

                if (
                        features == null
                                ||
                                features.length() == 0
                ) {

                    runOnUiThread(() -> {

                        if (requestId != searchRequestId) {
                            return;
                        }

                        Toast.makeText(
                                this,
                                "Location not found",
                                Toast.LENGTH_SHORT
                        ).show();
                    });

                    return;
                }

                JSONObject feature =
                        features.optJSONObject(0);

                if (feature == null) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Location not found",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                JSONObject geometry =
                        feature.optJSONObject(
                                "geometry"
                        );

                JSONObject properties =
                        feature.optJSONObject(
                                "properties"
                        );

                if (
                        geometry == null
                                ||
                                properties == null
                ) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Location not found",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                JSONArray coordinates =
                        geometry.optJSONArray(
                                "coordinates"
                        );

                if (
                        coordinates == null
                                ||
                                coordinates.length() < 2
                ) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Location not found",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                // GeoJSON order is [longitude, latitude].
                double longitude =
                        coordinates.optDouble(0, Double.NaN);

                double latitude =
                        coordinates.optDouble(1, Double.NaN);

                if (
                        Double.isNaN(latitude)
                                ||
                                Double.isNaN(longitude)
                ) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Location not found",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                String name =
                        properties.optString(
                                "name",
                                ""
                        ).trim();

                String street =
                        properties.optString(
                                "street",
                                ""
                        ).trim();

                String housenumber =
                        properties.optString(
                                "housenumber",
                                ""
                        ).trim();

                String locality =
                        properties.optString(
                                "locality",
                                ""
                        ).trim();

                String district =
                        properties.optString(
                                "district",
                                ""
                        ).trim();

                String city =
                        properties.optString(
                                "city",
                                ""
                        ).trim();

                String state =
                        properties.optString(
                                "state",
                                ""
                        ).trim();

                String country =
                        properties.optString(
                                "country",
                                ""
                        ).trim();

                String address =
                        buildPhotonAddress(
                                street,
                                housenumber,
                                locality,
                                district,
                                city,
                                state,
                                country
                        );

                if (name.isEmpty()) {

                    if (!street.isEmpty()) {
                        name = street;

                    } else if (!locality.isEmpty()) {
                        name = locality;

                    } else if (!city.isEmpty()) {
                        name = city;

                    } else {
                        name = "Selected location";
                    }
                }

                PhotonPlace place =
                        new PhotonPlace(
                                name,
                                address,
                                latitude,
                                longitude
                        );

                runOnUiThread(() -> {

                    if (requestId != searchRequestId) {
                        return;
                    }

                    selectPhotonPlace(place);

                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (requestId != searchRequestId) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            "Search error. Please check connection.",
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }


    private String normalizeSearchText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.getDefault());
    }


    // =========================================================
    // OFFLINE LIVE SUGGESTIONS
    // =========================================================

    private void loadOfflineDropdownSuggestions(
            String query
    ) {

        String lowerQuery =
                normalizeSearchText(query);

        ArrayList<PhotonPlace> results =
                new ArrayList<>();

        for (
                LocalPlace place :
                offlinePlaces
        ) {

            if (place == null) {
                continue;
            }

            String name =
                    place.name == null
                            ? ""
                            : place.name;

            String address =
                    place.address == null
                            ? ""
                            : place.address;

            String combined =
                    normalizeSearchText(
                            name
                                    + " "
                                    + address
                    );

            if (
                    combined.contains(
                            lowerQuery
                    )
            ) {

                results.add(
                        new PhotonPlace(
                                name,
                                address,
                                place.latitude,
                                place.longitude
                        )
                );
            }

            if (results.size() >= 10) {
                break;
            }
        }

        photonSuggestions.clear();
        photonSuggestions.addAll(results);

        suggestionAdapter.clear();
        suggestionAdapter.addAll(results);
        suggestionAdapter.notifyDataSetChanged();

        if (
                !results.isEmpty()
                        &&
                        etDestination.hasFocus()
        ) {

            etDestination.showDropDown();

        } else {

            etDestination.dismissDropDown();
        }
    }


    // =========================================================
    // OFFLINE SEARCH
    // =========================================================

    private void searchOfflinePlaces(
            String query
    ) {

        ArrayList<LocalPlace> matches =
                new ArrayList<>();

        String lowerQuery =
                normalizeSearchText(query);

        for (
                LocalPlace place :
                offlinePlaces
        ) {

            if (place == null) {
                continue;
            }

            String name =
                    normalizeSearchText(
                            place.name
                    );

            String address =
                    normalizeSearchText(
                            place.address
                    );

            if (
                    name.equals(lowerQuery)
                            ||
                            name.contains(lowerQuery)
                            ||
                            address.contains(lowerQuery)
            ) {

                matches.add(place);
            }
        }

        if (matches.isEmpty()) {

            Toast.makeText(
                    this,
                    "Location not found. Try the full place name or address.",
                    Toast.LENGTH_LONG
            ).show();

            tvRouteStatus.setText(
                    "Location not found."
            );

            return;
        }

        LocalPlace place =
                matches.get(0);

        selectedDestination =
                new GeoPoint(
                        place.latitude,
                        place.longitude
                );

        selectedDestinationName =
                place.name;

        suppressSearchWatcher = true;

        etDestination.setText(
                selectedDestinationName
        );

        etDestination.setSelection(
                etDestination.length()
        );

        suppressSearchWatcher = false;

        photonSuggestions.clear();
        suggestionAdapter.clear();

        etDestination.dismissDropDown();

        showDestinationOnMap();

        showDestinationPanel(
                place.address
        );

        tvDestinationAddress.setText(
                place.address
        );

        tvRouteStatus.setText(
                "Destination identified."
        );
    }


    // =========================================================
    // DESTINATION PANEL
    // =========================================================

    private void showDestinationPanel(
            String address
    ) {

        destinationPanel.setVisibility(
                View.VISIBLE
        );


        navigationPanel.setVisibility(
                View.GONE
        );


        layoutDirections.setVisibility(
                View.GONE
        );


        btnStartNavigation.setVisibility(
                View.GONE
        );


        layoutRouteOptions.setVisibility(
                View.GONE
        );


        tvDestinationName.setText(
                selectedDestinationName
        );


        if (
                address != null
                        &&
                        !address.trim().isEmpty()
        ) {

            tvDestinationAddress.setText(
                    address
            );

        } else {

            tvDestinationAddress.setText(
                    "Selected destination"
            );
        }


        selectedTransportMode =
                "car";


        updateTransportButtons();


        resetRouteButtons();


        mapView.invalidate();
    }


    // =========================================================
    // TRANSPORT MODES
    // =========================================================

    private void setupTransportModes() {

        btnCar.setOnClickListener(v -> {

            selectedTransportMode =
                    "car";

            updateTransportButtons();

            showRouteOptionsForMode();
        });


        btnWalk.setOnClickListener(v -> {

            selectedTransportMode =
                    "walking";

            updateTransportButtons();

            showRouteOptionsForMode();
        });


        btnBike.setOnClickListener(v -> {

            selectedTransportMode =
                    "bicycle";

            updateTransportButtons();

            showRouteOptionsForMode();
        });


        btnTransit.setOnClickListener(v -> {

            selectedTransportMode =
                    "transit";

            updateTransportButtons();

            showTransitOption();
        });
    }


    private void updateTransportButtons() {

        resetTransportButton(btnCar);

        resetTransportButton(btnWalk);

        resetTransportButton(btnBike);

        resetTransportButton(btnTransit);


        if (
                selectedTransportMode.equals(
                        "car"
                )
        ) {

            selectTransportButton(
                    btnCar
            );

        } else if (
                selectedTransportMode.equals(
                        "walking"
                )
        ) {

            selectTransportButton(
                    btnWalk
            );

        } else if (
                selectedTransportMode.equals(
                        "bicycle"
                )
        ) {

            selectTransportButton(
                    btnBike
            );

        } else {

            selectTransportButton(
                    btnTransit
            );
        }
    }


    private void resetTransportButton(
            MaterialButton button
    ) {

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.WHITE
                )
        );


        button.setTextColor(
                Color.rgb(
                        26,
                        26,
                        46
                )
        );
    }


    private void selectTransportButton(
            MaterialButton button
    ) {

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.rgb(
                                255,
                                240,
                                245
                        )
                )
        );


        button.setTextColor(
                Color.rgb(
                        233,
                        30,
                        99
                )
        );
    }


    private void showRouteOptionsForMode() {

        layoutRouteOptions.setVisibility(
                View.VISIBLE
        );


        btnSafestRoute.setText(
                "🛡 Safest\nChoose"
        );


        btnFastestRoute.setText(
                "⚡ Fastest\nChoose"
        );


        layoutDirections.setVisibility(
                View.GONE
        );


        btnStartNavigation.setVisibility(
                View.GONE
        );


        String modeText =
                getTransportDisplayName();


        tvRouteStatus.setText(
                "Choose a route for "
                        + modeText
                        + "."
        );
    }


    private void showTransitOption() {

        layoutRouteOptions.setVisibility(
                View.GONE
        );


        layoutDirections.setVisibility(
                View.GONE
        );


        btnStartNavigation.setVisibility(
                View.VISIBLE
        );


        btnStartNavigation.setText(
                "Open Transit Directions"
        );


        tvRouteStatus.setText(
                "Public transport routing will open with your destination."
        );
    }


    private String getTransportDisplayName() {

        if (
                selectedTransportMode.equals(
                        "walking"
                )
        ) {

            return "walking";
        }


        if (
                selectedTransportMode.equals(
                        "bicycle"
                )
        ) {

            return "bicycle";
        }


        if (
                selectedTransportMode.equals(
                        "transit"
                )
        ) {

            return "public transport";
        }


        return "car";
    }


    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        fabMyLocation.setOnClickListener(
                v -> goToMyLocation()
        );


        btnSafestRoute.setOnClickListener(
                v -> calculateRoute(true)
        );


        btnFastestRoute.setOnClickListener(
                v -> calculateRoute(false)
        );


        btnStartNavigation.setOnClickListener(
                v -> {

                    if (
                            selectedTransportMode.equals(
                                    "transit"
                            )
                    ) {

                        openTransitDirections();

                    } else {

                        startNavigation();
                    }
                }
        );


        btnStopNavigation.setOnClickListener(
                v -> stopNavigation()
        );


        btnTriggerSOS.setOnClickListener(
                v -> confirmSOS()
        );
    }


    // =========================================================
    // ROUTING
    // =========================================================

    private void calculateRoute(
            boolean safest
    ) {

        GeoPoint start =
                getBestAvailableLocation();


        if (start == null) {

            Toast.makeText(
                    this,
                    "Waiting for your GNSS/GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (selectedDestination == null) {

            Toast.makeText(
                    this,
                    "Select a destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // Transit still uses the existing external directions flow.
        // The local GraphHopper road graph is for car/walking/bicycle.
        if (selectedTransportMode.equals("transit")) {

            if (!isNetworkAvailable()) {

                new AlertDialog.Builder(this)
                        .setTitle("Transit offline")
                        .setMessage(
                                "Offline public-transit routing is not available in this version.\n\n"
                                        + "Choose Car, Walk or Bike for offline road routing."
                        )
                        .setPositiveButton("OK", null)
                        .show();

            } else {

                openTransitDirections();
            }

            return;
        }


        if (!isNetworkAvailable()) {

            // TRUE offline route: GNSS start + local road graph + destination.
            requestOfflineRoute(
                    start,
                    selectedDestination,
                    safest
            );

            return;
        }


        tvRouteStatus.setText(
                safest
                        ? "Finding safer route..."
                        : "Finding fastest route..."
        );


        requestOnlineRoute(
                start,
                selectedDestination,
                safest
        );
    }


    // =========================================================
    // TRUE OFFLINE GRAPH INITIALIZATION
    // =========================================================

    private void prepareOfflineRoutingEngine() {

        offlineRoutingExecutor.execute(() -> {

            try {

                File graphFolder =
                        new File(
                                getFilesDir(),
                                OFFLINE_GRAPH_FOLDER_NAME
                        );


                if (!hasOfflineGraphFiles(graphFolder)) {

                    copyAssetFolderToInternalStorage(
                            OFFLINE_GRAPH_ASSET_FOLDER,
                            graphFolder
                    );
                }


                if (!hasOfflineGraphFiles(graphFolder)) {

                    throw new IOException(
                            "No GraphHopper graph was found. Put the generated graph files in app/src/main/assets/graphhopper/."
                    );
                }


                GraphHopper hopper =
                        new GraphHopper()
                                .forMobile();

                // The graph is read-only on the phone.
                hopper.setAllowWrites(false);

                // Keep the graph flexible because the same graph is used
                // for car, walking and bicycle requests.

                hopper.setEnableCalcPoints(true);

                // Memory-mapped storage keeps large local road graphs from
                // consuming as much Java heap on the phone.
                hopper.setMemoryMapped();

                hopper.setEncodingManager(
                        EncodingManager.create(
                                "car,foot,bike"
                        )
                );

                boolean loaded =
                        hopper.load(
                                graphFolder.getAbsolutePath()
                        );


                if (!loaded) {

                    hopper.close();

                    throw new IOException(
                            "GraphHopper could not load the offline road graph."
                    );
                }


                offlineHopper = hopper;
                offlineRouterReady = true;
                offlineRouterFailed = false;
                offlineRouterError = "";

            } catch (Throwable error) {

                offlineRouterReady = false;
                offlineRouterFailed = true;
                offlineRouterError =
                        error.getMessage() == null
                                ? error.toString()
                                : error.getMessage();
            }
        });
    }


    private boolean hasOfflineGraphFiles(
            File graphFolder
    ) {

        if (graphFolder == null || !graphFolder.isDirectory()) {
            return false;
        }

        // GraphHopper always creates a properties file in a valid graph
        // directory. Checking this prevents an incomplete asset copy from
        // being treated as a usable graph.
        File properties =
                new File(graphFolder, "properties");

        return properties.isFile() && properties.length() > 0;
    }


    private void copyAssetFolderToInternalStorage(
            String assetFolder,
            File destinationFolder
    ) throws IOException {

        AssetManager assetManager = getAssets();

        String[] children =
                assetManager.list(assetFolder);


        if (children == null || children.length == 0) {
            return;
        }


        if (!destinationFolder.exists()
                && !destinationFolder.mkdirs()) {

            throw new IOException(
                    "Unable to create offline graph folder: "
                            + destinationFolder.getAbsolutePath()
            );
        }


        for (String child : children) {

            String assetPath =
                    assetFolder + "/" + child;

            File output =
                    new File(
                            destinationFolder,
                            child
                    );


            String[] nested =
                    assetManager.list(assetPath);


            if (nested != null && nested.length > 0) {

                copyAssetFolderToInternalStorage(
                        assetPath,
                        output
                );

            } else {

                copyAssetFile(
                        assetPath,
                        output
                );
            }
        }
    }


    private void copyAssetFile(
            String assetPath,
            File outputFile
    ) throws IOException {

        File parent = outputFile.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }


        try (java.io.InputStream input =
                     getAssets().open(assetPath);
             FileOutputStream output =
                     new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[16 * 1024];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();
        }
    }


    // =========================================================
    // TRUE OFFLINE ROUTING
    // =========================================================

    private void requestOfflineRoute(
            GeoPoint start,
            GeoPoint destination,
            boolean safest
    ) {

        tvRouteStatus.setText(
                safest
                        ? "Calculating safer offline route..."
                        : "Calculating offline route..."
        );


        offlineRoutingExecutor.execute(() -> {

            try {

                GraphHopper hopper = offlineHopper;


                if (!offlineRouterReady || hopper == null) {

                    if (offlineRouterFailed) {

                        throw new IOException(
                                offlineRouterError
                        );
                    }

                    throw new IOException(
                            "Offline road data is still loading. Please wait a few seconds and try again."
                    );
                }


                String vehicle;

                if (selectedTransportMode.equals("walking")) {
                    vehicle = "foot";
                } else if (selectedTransportMode.equals("bicycle")) {
                    vehicle = "bike";
                } else {
                    vehicle = "car";
                }


                GHRequest request = new GHRequest(
                        start.getLatitude(),
                        start.getLongitude(),
                        destination.getLatitude(),
                        destination.getLongitude()
                )
                        .setProfile(vehicle)
                        .setLocale(Locale.US);

                // For the offline SAFE route button we ask the local graph
                // for a few alternatives and then apply SheShield's same
                // incident-zone rule used by the online route selector.
                // GraphHopper alternative routing requires CH to be disabled,
                // which is why the offline graph is loaded in flexible mode.
                if (safest) {
                    request.setAlgorithm(
                            Parameters.Algorithms.ALT_ROUTE
                    );
                    request.getHints().put(
                            Parameters.Algorithms.AltRoute.MAX_PATHS,
                            "3"
                    );
                }

                // No internet request happens here. GraphHopper reads the
                // preloaded local road graph only.
                GHResponse response =
                        hopper.route(request);

                // Some graph builds may not contain alternative-route
                // preparation. If that happens, gracefully fall back to
                // the normal fastest local road route instead of failing.
                if (safest && response.hasErrors()) {

                    request =
                            new GHRequest(
                                    start.getLatitude(),
                                    start.getLongitude(),
                                    destination.getLatitude(),
                                    destination.getLongitude()
                            )


                                    .setProfile(vehicle)
                                    .setWeighting("fastest")
                                    .setLocale(Locale.US);

                    response = hopper.route(request);
                }


                if (response.hasErrors()) {

                    throw new IOException(
                            response.getErrors().toString()
                    );
                }


                if (response.hasErrors()) {

                    throw new IOException(
                            "No offline road route was found between your current location and the destination."
                    );
                }


                List<PathWrapper> candidatePaths =
                        response.getAll();

                if (candidatePaths == null || candidatePaths.isEmpty()) {
                    candidatePaths =
                            java.util.Collections.singletonList(
                                    response.getBest()
                            );
                }


                PathWrapper bestPath = null;
                ArrayList<GeoPoint> points = null;
                double distance = Double.MAX_VALUE;
                double durationSeconds = Double.MAX_VALUE;
                int incidentHits = Integer.MAX_VALUE;

                for (PathWrapper candidate : candidatePaths) {

                    if (candidate == null || candidate.getPoints() == null
                            || candidate.getPoints().size() < 2) {
                        continue;
                    }

                    ArrayList<GeoPoint> candidatePoints =
                            convertGraphHopperPoints(
                                    candidate.getPoints()
                            );

                    int candidateIncidentHits =
                            countIncidentZoneHits(candidatePoints);

                    double candidateDistance =
                            candidate.getDistance();

                    double candidateDuration =
                            candidate.getTime() / 1000.0;

                    boolean better;

                    if (safest) {
                        better =
                                candidateIncidentHits < incidentHits
                                        || (candidateIncidentHits == incidentHits
                                        && candidateDuration < durationSeconds);
                    } else {
                        better =
                                candidateDuration < durationSeconds;
                    }

                    if (bestPath == null || better) {
                        bestPath = candidate;
                        points = candidatePoints;
                        distance = candidateDistance;
                        durationSeconds = candidateDuration;
                        incidentHits = candidateIncidentHits;
                    }
                }


                if (bestPath == null || points == null || points.size() < 2) {
                    throw new IOException(
                            "Offline routing returned an empty route."
                    );
                }


                String finalMode =
                        selectedTransportMode;

                saveRoute(
                        points,
                        destination,
                        selectedDestinationName
                );


                final double finalDistance = distance;
                final double finalDurationSeconds = durationSeconds;
                final ArrayList<GeoPoint> finalPoints = new ArrayList<>(points);
                final int finalIncidentHits = incidentHits;

                runOnUiThread(() -> {

                    selectedRouteDistanceMeters =
                            finalDistance;

                    selectedRouteDurationSeconds =
                            finalDurationSeconds;

                    currentRoutePoints.clear();
                    currentRoutePoints.addAll(finalPoints);

                    drawRoute(finalPoints);

                    showDestinationOnMap();

                    tvTravelInfo.setText(
                            formatDistance(finalDistance)                                    + " · "
                                    + formatDuration(finalDurationSeconds)
                    );

                    tvTravelMode.setText(
                            getModeEmoji(finalMode)
                                    + " "
                                    + getModeLabel(finalMode)
                    );

                    tvRouteType.setText(
                            safest
                                    ? "Offline · Road route"
                                    : "Offline · Fastest road route"
                    );

                    tvRouteStatus.setText(
                            safest
                                    ? "Offline road route ready. Incident zones on route: " + finalIncidentHits
                                    : "Offline road route ready."
                    );

                    layoutDirections.setVisibility(
                            View.VISIBLE
                    );

                    btnStartNavigation.setVisibility(
                            View.VISIBLE
                    );

                    layoutRouteOptions.setVisibility(
                            View.GONE
                    );

                    if (isNetworkAvailable()) {
                        tvRouteStatus.setText(
                                "Offline road route ready."
                        );
                    }

                });

            } catch (Throwable error) {

                String message =
                        error.getMessage() == null
                                ? error.toString()
                                : error.getMessage();

                runOnUiThread(() -> {

                    tvRouteStatus.setText(
                            "Offline routing unavailable."
                    );

                    showOfflineRouteMessage(
                            message
                    );
                });
            }
        });
    }


    private ArrayList<GeoPoint> convertGraphHopperPoints(
            PointList pointList
    ) {

        ArrayList<GeoPoint> points =
                new ArrayList<>();

        if (pointList == null) {
            return points;
        }

        for (int i = 0; i < pointList.size(); i++) {
            points.add(
                    new GeoPoint(
                            pointList.getLatitude(i),
                            pointList.getLongitude(i)
                    )
            );
        }

        return points;
    }


    private void requestOnlineRoute(
            GeoPoint start,
            GeoPoint destination,
            boolean safest
    ) {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

            try {

                String coordinates =
                        start.getLongitude()
                                + ","
                                + start.getLatitude()
                                + ";"
                                + destination.getLongitude()
                                + ","
                                + destination.getLatitude();


                String service;


                if (
                        selectedTransportMode.equals(
                                "walking"
                        )
                ) {

                    service =
                            "https://routing.openstreetmap.de/routed-foot/route/v1/driving/";

                } else if (
                        selectedTransportMode.equals(
                                "bicycle"
                        )
                ) {

                    service =
                            "https://routing.openstreetmap.de/routed-bike/route/v1/driving/";

                } else {

                    service =
                            "https://routing.openstreetmap.de/routed-car/route/v1/driving/";
                }


                String urlString =
                        service
                                + coordinates
                                + "?overview=full"
                                + "&geometries=geojson"
                                + "&steps=true"
                                + "&alternatives=true";


                URL url =
                        new URL(
                                urlString
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "GET"
                );


                connection.setConnectTimeout(
                        15000
                );


                connection.setReadTimeout(
                        20000
                );


                connection.setUseCaches(
                        false
                );


                connection.setRequestProperty(
                        "User-Agent",
                        "SheShield Android App"
                );


                int responseCode =
                        connection.getResponseCode();


                InputStream stream;


                if (
                        responseCode >= 200
                                &&
                                responseCode < 300
                ) {

                    stream =
                            connection.getInputStream();

                } else {

                    stream =
                            connection.getErrorStream();
                }


                StringBuilder result =
                        new StringBuilder();


                if (stream != null) {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            stream
                                    )
                            );


                    String line;


                    while (
                            (line =
                                    reader.readLine())
                                    != null
                    ) {

                        result.append(
                                line
                        );
                    }


                    reader.close();
                }


                if (
                        responseCode < 200
                                ||
                                responseCode >= 300
                ) {

                    throw new Exception(
                            "Routing server error: "
                                    + responseCode
                    );
                }


                JSONObject json =
                        new JSONObject(
                                result.toString()
                        );


                String code =
                        json.optString(
                                "code"
                        );


                if (
                        !"Ok".equalsIgnoreCase(
                                code
                        )
                ) {

                    throw new Exception(
                            "Route could not be found."
                    );
                }


                JSONArray routes =
                        json.getJSONArray(
                                "routes"
                        );


                if (routes.length() == 0) {

                    throw new Exception(
                            "No route available."
                    );
                }


                // Collect real route candidates from the existing routing service.
                // The API ranks these candidates; it does not create road geometry.
                JSONObject bestRoute = null;
                ArrayList<GeoPoint> bestPoints = null;
                double bestDistance = Double.MAX_VALUE;
                double bestDuration = Double.MAX_VALUE;
                int bestIncidentHits = Integer.MAX_VALUE;

                ArrayList<JSONObject> candidateRoutes = new ArrayList<>();
                ArrayList<ArrayList<GeoPoint>> candidatePointLists = new ArrayList<>();
                ArrayList<Integer> candidateIncidentHits = new ArrayList<>();

                for (int routeIndex = 0; routeIndex < routes.length(); routeIndex++) {
                    JSONObject candidate = routes.getJSONObject(routeIndex);
                    JSONObject candidateGeometry = candidate.getJSONObject("geometry");
                    JSONArray candidateCoordinates = candidateGeometry.getJSONArray("coordinates");

                    ArrayList<GeoPoint> candidatePoints = new ArrayList<>();
                    for (int i = 0; i < candidateCoordinates.length(); i++) {
                        JSONArray point = candidateCoordinates.getJSONArray(i);
                        candidatePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                    }

                    if (candidatePoints.size() < 2) {
                        continue;
                    }

                    candidateRoutes.add(candidate);
                    candidatePointLists.add(candidatePoints);
                    candidateIncidentHits.add(countIncidentZoneHits(candidatePoints));
                }

                int selectedIndex = -1;

                // Use the experimental API for the Safest option while online.
                // Risk is a proxy derived from the existing marked incident zones.
                // If the API is unavailable, keep the existing local selection logic.
                if (safest && !candidateRoutes.isEmpty()) {
                    selectedIndex = rankRoutesWithSheShieldApi(
                            candidateRoutes,
                            candidateIncidentHits
                    );
                }

                if (selectedIndex < 0) {
                    for (int i = 0; i < candidateRoutes.size(); i++) {
                        JSONObject candidate = candidateRoutes.get(i);
                        double candidateDistance = candidate.getDouble("distance");
                        double candidateDuration = candidate.getDouble("duration");
                        int incidentHits = candidateIncidentHits.get(i);

                        boolean better;
                        if (safest) {
                            better = bestRoute == null
                                    || incidentHits < bestIncidentHits
                                    || (incidentHits == bestIncidentHits
                                    && candidateDuration < bestDuration);
                        } else {
                            better = bestRoute == null || candidateDuration < bestDuration;
                        }

                        if (better) {
                            bestRoute = candidate;
                            bestPoints = candidatePointLists.get(i);
                            bestDistance = candidateDistance;
                            bestDuration = candidateDuration;
                            bestIncidentHits = incidentHits;
                        }
                    }
                } else {
                    bestRoute = candidateRoutes.get(selectedIndex);
                    bestPoints = candidatePointLists.get(selectedIndex);
                    bestDistance = bestRoute.getDouble("distance");
                    bestDuration = bestRoute.getDouble("duration");
                    bestIncidentHits = candidateIncidentHits.get(selectedIndex);
                }

                if (bestRoute == null || bestPoints == null || bestPoints.isEmpty()) {
                    throw new Exception("No usable route available.");
                }

                double distance = bestDistance;
                double duration = bestDuration;
                ArrayList<GeoPoint> points = bestPoints;

                saveRoute(
                        points,
                        destination,
                        selectedDestinationName
                );


                String finalMode =
                        selectedTransportMode;


                runOnUiThread(() -> {

                    selectedRouteDistanceMeters =
                            distance;


                    selectedRouteDurationSeconds =
                            duration;


                    currentRoutePoints.clear();
                    currentRoutePoints.addAll(points);


                    drawRoute(
                            points
                    );


                    String routeLabel;


                    if (safest) {

                        routeLabel =
                                "Safer option";

                    } else {

                        routeLabel =
                                "Fastest";
                    }


                    String info =
                            formatDistance(
                                    distance
                            )
                                    + " · "
                                    + formatDuration(
                                    duration
                            );


                    tvTravelInfo.setText(
                            info
                    );


                    tvTravelMode.setText(
                            getModeEmoji(
                                    finalMode
                            )
                                    + " "
                                    + getModeLabel(
                                    finalMode
                            )
                    );


                    tvRouteType.setText(
                            routeLabel
                    );


                    layoutDirections.setVisibility(
                            View.VISIBLE
                    );


                    btnStartNavigation.setVisibility(
                            View.VISIBLE
                    );


                    btnStartNavigation.setText(
                            "Start Navigation"
                    );


                    tvRouteStatus.setText(
                            routeLabel
                                    + " • "
                                    + info
                    );


                    Toast.makeText(
                            this,
                            routeLabel
                                    + " ready.",
                            Toast.LENGTH_SHORT
                    ).show();
                });


            } catch (Exception e) {

                String error =
                        e.getMessage();


                if (
                        error == null
                                ||
                                error.trim().isEmpty()
                ) {

                    error =
                            "Unknown routing error.";
                }


                String finalError =
                        error;


                runOnUiThread(() -> {

                    tvRouteStatus.setText(
                            "Unable to calculate route."
                    );


                    new AlertDialog.Builder(this)
                            .setTitle(
                                    "Route unavailable"
                            )
                            .setMessage(
                                    finalError
                            )
                            .setPositiveButton(
                                    "OK",
                                    null
                            )
                            .show();
                });


            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    /**
     * Ranks the actual online route candidates using the experimental API.
     * Returns the zero-based candidate index, or -1 to use local fallback.
     *
     * risk_score is currently a proxy based on marked incident-zone hits,
     * not a live or independently verified road-safety measurement.
     */
    private int rankRoutesWithSheShieldApi(
            ArrayList<JSONObject> candidateRoutes,
            ArrayList<Integer> incidentHits
    ) {
        HttpURLConnection connection = null;

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray routeArray = new JSONArray();

            for (int i = 0; i < candidateRoutes.size(); i++) {
                JSONObject candidate = candidateRoutes.get(i);
                JSONObject route = new JSONObject();

                route.put("name", "Route " + i);
                route.put("distance_km",
                        Math.max(0.001, candidate.getDouble("distance") / 1000.0));

                int hits = incidentHits.get(i);
                double riskScore = Math.max(1.0, Math.min(10.0, 1.0 + hits));
                route.put("risk_score", riskScore);
                routeArray.put(route);
            }

            requestBody.put("routes", routeArray);
            requestBody.put("risk_weight", 0.7);

            URL url = new URL(SHE_SHIELD_ROUTE_API);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");

            byte[] payload = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(payload);
                output.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                Log.w("SheShieldRouteAPI", "API returned HTTP " + responseCode);
                return -1;
            }

            StringBuilder responseText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
            }

            JSONObject response = new JSONObject(responseText.toString());
            JSONArray rankedResults = response.optJSONArray("results");
            if (rankedResults == null || rankedResults.length() == 0) {
                return -1;
            }

            String topName = rankedResults.getJSONObject(0).optString("name", "");
            if (!topName.startsWith("Route ")) {
                return -1;
            }

            int selected = Integer.parseInt(topName.substring("Route ".length()));
            if (selected < 0 || selected >= candidateRoutes.size()) {
                return -1;
            }

            Log.d("SheShieldRouteAPI",
                    "API ranked " + topName
                            + " first; risk is estimated from incident-zone hits.");
            return selected;

        } catch (Exception e) {
            Log.w("SheShieldRouteAPI",
                    "Route ranking API unavailable; using local fallback.", e);
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private String getModeLabel(
            String mode
    ) {

        if (mode.equals("walking")) {
            return "Walk";
        }


        if (mode.equals("bicycle")) {
            return "Bike";
        }


        if (mode.equals("transit")) {
            return "Transit";
        }


        return "Car";
    }


    private String getModeEmoji(
            String mode
    ) {

        if (mode.equals("walking")) {
            return "🚶";
        }


        if (mode.equals("bicycle")) {
            return "🚲";
        }


        if (mode.equals("transit")) {
            return "🚆";
        }


        return "🚗";
    }


    // =========================================================
    // TRANSIT
    // =========================================================

    private void openTransitDirections() {

        if (selectedDestination == null) {

            Toast.makeText(
                    this,
                    "Select a destination first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        GeoPoint start =
                getBestAvailableLocation();


        if (start == null) {

            Toast.makeText(
                    this,
                    "Waiting for your GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String uri =
                "https://www.google.com/maps/dir/?api=1"
                        + "&origin="
                        + start.getLatitude()
                        + ","
                        + start.getLongitude()
                        + "&destination="
                        + selectedDestination.getLatitude()
                        + ","
                        + selectedDestination.getLongitude()
                        + "&travelmode=transit";


        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(uri)
                    );


            startActivity(
                    intent
            );


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Transit directions are not available on this device.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    private void startNavigation() {

        if (
                routeLine == null
                        ||
                        selectedDestination == null
        ) {

            Toast.makeText(
                    this,
                    "Choose a route first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        navigationStarted = true;

        // Start a fresh progress calculation for this navigation session.
        lastNavigationRemainingMeters = selectedRouteDistanceMeters;
        lastNavigationSegmentIndex = 0;

        destinationPanel.setVisibility(
                View.GONE
        );


        navigationPanel.setVisibility(
                View.VISIBLE
        );


        tvNavigationTitle.setText(
                "Navigation started"
        );


        tvNavigationInfo.setText(
                formatDistance(
                        selectedRouteDistanceMeters
                )
                        + " · "
                        + formatDuration(
                        selectedRouteDurationSeconds
                )
        );


        if (myLocationOverlay != null) {

            myLocationOverlay
                    .enableFollowLocation();
        }


        tvRouteStatus.setText(
                "Navigation active"
        );


        startNavigationUpdates();


        Toast.makeText(
                this,
                "Navigation started.",
                Toast.LENGTH_SHORT
        ).show();
    }


    private void startNavigationUpdates() {

        if (navigationUpdater != null) {

            mainHandler.removeCallbacks(
                    navigationUpdater
            );
        }


        navigationUpdater =
                new Runnable() {

                    @Override
                    public void run() {

                        if (!navigationStarted) {
                            return;
                        }


                        GeoPoint current =
                                getBestAvailableLocation();


                        if (
                                current != null
                                        &&
                                        selectedDestination != null
                        ) {

                            double remainingDistance =
                                    getRemainingRouteDistance(current);


                            double averageSpeed;


                            if (
                                    selectedRouteDurationSeconds
                                            > 0
                                            &&
                                            selectedRouteDistanceMeters
                                                    > 0
                            ) {

                                averageSpeed =
                                        selectedRouteDistanceMeters
                                                /
                                                selectedRouteDurationSeconds;

                            } else {

                                averageSpeed =
                                        getDefaultSpeed();
                            }


                            if (averageSpeed <= 0) {

                                averageSpeed =
                                        getDefaultSpeed();
                            }


                            double remainingSeconds =
                                    remainingDistance
                                            /
                                            averageSpeed;


                            tvNavigationInfo.setText(
                                    formatDistance(
                                            remainingDistance
                                    )
                                            + " remaining · "
                                            + formatDuration(
                                            remainingSeconds
                                    )
                            );


                            if (
                                    remainingDistance < 30
                            ) {

                                tvNavigationTitle.setText(
                                        "You are near your destination"
                                );

                            } else {

                                tvNavigationTitle.setText(
                                        "Navigation active"
                                );
                            }
                        }


                        mainHandler.postDelayed(
                                this,
                                2000
                        );
                    }
                };


        mainHandler.post(
                navigationUpdater
        );
    }


    private double calculatePolylineDistance(
            ArrayList<GeoPoint> points
    ) {

        if (points == null || points.size() < 2) {
            return 0;
        }

        double total = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            total += points.get(i).distanceToAsDouble(
                    points.get(i + 1)
            );
        }

        return total;
    }


    /**
     * Calculates remaining distance along the actual route geometry.
     * This is more accurate than current.distanceTo(destination),
     * especially when roads curve or the route has turns.
     */
    private double getRemainingRouteDistance(GeoPoint current) {

        if (current == null || selectedDestination == null) {
            return 0;
        }

        if (currentRoutePoints.size() < 2) {
            return current.distanceToAsDouble(selectedDestination);
        }

        /*
         * Project the GPS point onto the actual route segments instead of
         * choosing the nearest route vertex. This is much more stable on
         * curved roads and when route points are far apart.
         */
        int startSegment = Math.max(0, lastNavigationSegmentIndex - 2);
        double bestDistanceToRoute = Double.MAX_VALUE;
        double bestRemaining = Double.MAX_VALUE;
        int bestSegment = lastNavigationSegmentIndex;

        double travelledBeforeSegment = 0.0;

        // We need the distance before each segment. Build it once as we scan.
        double[] cumulative = new double[currentRoutePoints.size()];
        cumulative[0] = 0.0;

        for (int i = 1; i < currentRoutePoints.size(); i++) {
            cumulative[i] = cumulative[i - 1]
                    + currentRoutePoints.get(i - 1)
                    .distanceToAsDouble(currentRoutePoints.get(i));
        }

        for (int i = startSegment; i < currentRoutePoints.size() - 1; i++) {

            GeoPoint a = currentRoutePoints.get(i);
            GeoPoint b = currentRoutePoints.get(i + 1);

            double latScale =
                    111320.0;
            double lonScale =
                    111320.0 * Math.cos(
                            Math.toRadians(current.getLatitude())
                    );

            double ax = (a.getLongitude() - current.getLongitude()) * lonScale;
            double ay = (a.getLatitude() - current.getLatitude()) * latScale;
            double bx = (b.getLongitude() - current.getLongitude()) * lonScale;
            double by = (b.getLatitude() - current.getLatitude()) * latScale;

            double dx = bx - ax;
            double dy = by - ay;
            double lengthSquared = dx * dx + dy * dy;

            double t;
            if (lengthSquared <= 0.0001) {
                t = 0.0;
            } else {
                t = -(ax * dx + ay * dy) / lengthSquared;
                t = Math.max(0.0, Math.min(1.0, t));
            }

            double px = ax + t * dx;
            double py = ay + t * dy;
            double distanceToSegment = Math.sqrt(px * px + py * py);

            // Ignore an implausible GPS jump far away from the route.
            if (distanceToSegment > 100.0) {
                continue;
            }

            double segmentLength =
                    a.distanceToAsDouble(b);

            double remaining =
                    cumulative[i]
                            + (1.0 - t) * segmentLength;

            /*
             * Prefer the geometrically closest segment, but never let normal
             * GPS noise make navigation jump backwards by a large amount.
             */
            if (remaining > lastNavigationRemainingMeters + 35.0) {
                continue;
            }

            if (distanceToSegment < bestDistanceToRoute) {
                bestDistanceToRoute = distanceToSegment;
                bestRemaining = remaining;
                bestSegment = i;
            }
        }

        if (bestRemaining == Double.MAX_VALUE) {
            // GPS temporarily lost the route. Keep the last valid progress.
            return lastNavigationRemainingMeters > 0
                    ? lastNavigationRemainingMeters
                    : current.distanceToAsDouble(selectedDestination);
        }

        lastNavigationSegmentIndex = bestSegment;

        if (lastNavigationRemainingMeters < 0) {
            lastNavigationRemainingMeters = bestRemaining;
        } else {
            // Allow normal forward progress, but suppress small backward jumps.
            if (bestRemaining > lastNavigationRemainingMeters) {
                bestRemaining = lastNavigationRemainingMeters;
            }

            lastNavigationRemainingMeters =
                    Math.max(0.0, bestRemaining);
        }

        return lastNavigationRemainingMeters;
    }


    /** Counts how many route samples fall inside marked incident zones. */
    private int countIncidentZoneHits(ArrayList<GeoPoint> points) {

        if (points == null || points.isEmpty() || incidentOverlays.isEmpty()) {
            return 0;
        }

        int hits = 0;

        for (IncidentOverlay incident : incidentOverlays.values()) {
            boolean intersects = false;

            for (GeoPoint routePoint : points) {
                if (routePoint.distanceToAsDouble(incident.point)
                        <= incident.radiusMeters()) {
                    intersects = true;
                    break;
                }
            }

            if (intersects) {
                hits++;
            }
        }

        return hits;
    }


    private double getDefaultSpeed() {

        if (
                selectedTransportMode.equals(
                        "walking"
                )
        ) {

            // 5 km/h average walking speed.
            return 5.0 / 3.6;
        }


        if (
                selectedTransportMode.equals(
                        "bicycle"
                )
        ) {

            // 15 km/h average cycling speed.
            return 15.0 / 3.6;
        }


        // 30 km/h average city-driving speed for offline ETA.
        return 30.0 / 3.6;
    }


    private void stopNavigation() {

        navigationStarted = false;
        lastNavigationRemainingMeters = -1.0;
        lastNavigationSegmentIndex = 0;


        if (navigationUpdater != null) {

            mainHandler.removeCallbacks(
                    navigationUpdater
            );


            navigationUpdater =
                    null;
        }


        navigationPanel.setVisibility(
                View.GONE
        );


        destinationPanel.setVisibility(
                View.VISIBLE
        );


        tvRouteStatus.setText(
                "Navigation stopped."
        );
    }


    // =========================================================
    // ROUTE DRAWING
    // =========================================================

    private void drawRoute(
            ArrayList<GeoPoint> points
    ) {

        if (routeLine != null) {

            mapView.getOverlays()
                    .remove(
                            routeLine
                    );
        }


        routeLine =
                new Polyline();


        routeLine.setPoints(
                points
        );


        routeLine.setColor(
                Color.rgb(
                        251,
                        87,
                        146
                )
        );


        routeLine.setWidth(
                10f
        );


        mapView.getOverlays()
                .add(
                        routeLine
                );


        mapView.invalidate();
    }


    private void showDestinationOnMap() {

        if (selectedDestination == null) {
            return;
        }


        if (destinationMarker != null) {

            mapView.getOverlays()
                    .remove(
                            destinationMarker
                    );
        }


        destinationMarker =
                new Marker(
                        mapView
                );


        destinationMarker.setPosition(
                selectedDestination
        );


        destinationMarker.setTitle(
                selectedDestinationName
        );


        destinationMarker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
        );


        mapView.getOverlays()
                .add(
                        destinationMarker
                );


        mapView.getController()
                .animateTo(
                        selectedDestination
                );


        mapView.getController()
                .setZoom(
                        17.0
                );


        mapView.invalidate();
    }


    // =========================================================
    // OFFLINE PLACES
    // =========================================================

    private void loadOfflinePlaces() {

        offlinePlaces.clear();


        offlinePlaces.add(
                new LocalPlace(
                        "Presidency University",
                        "Yelahanka, Bengaluru",
                        13.1766,
                        77.5375
                )
        );


        offlinePlaces.add(
                new LocalPlace(
                        "Kempegowda International Airport",
                        "Devanahalli, Bengaluru",
                        13.1986,
                        77.7066
                )
        );


        offlinePlaces.add(
                new LocalPlace(
                        "Yelahanka",
                        "Bengaluru",
                        13.1007,
                        77.5963
                )
        );


        offlinePlaces.add(
                new LocalPlace(
                        "Hebbal",
                        "Bengaluru",
                        13.0358,
                        77.5970
                )
        );


        offlinePlaces.add(
                new LocalPlace(
                        "Bangalore City Railway Station",
                        "Majestic, Bengaluru",
                        12.9784,
                        77.5708
                )
        );


        offlinePlaces.add(
                new LocalPlace(
                        "Kempegowda Bus Station",
                        "Majestic, Bengaluru",
                        12.9767,
                        77.5713
                )
        );
    }


    // =========================================================
    // SAVED ROUTES
    // =========================================================

    private void saveRoute(
            ArrayList<GeoPoint> points,
            GeoPoint destination,
            String destinationName
    ) {

        StringBuilder routeData =
                new StringBuilder();


        for (GeoPoint point : points) {

            routeData.append(
                    point.getLatitude()
            );


            routeData.append(",");


            routeData.append(
                    point.getLongitude()
            );


            routeData.append(";");
        }


        SharedPreferences prefs =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );


        prefs.edit()
                .putString(
                        KEY_SAVED_ROUTE,
                        routeData.toString()
                )
                .putString(
                        KEY_SAVED_DESTINATION,
                        destination.getLatitude()
                                + ","
                                + destination.getLongitude()
                )
                .putString(
                        KEY_SAVED_DESTINATION_NAME,
                        destinationName
                )
                .putString(
                        KEY_SAVED_TRANSPORT_MODE,
                        selectedTransportMode
                )
                .apply();
    }


    private void loadSavedRoute() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );


        String destination =
                prefs.getString(
                        KEY_SAVED_DESTINATION,
                        ""
                );


        String name =
                prefs.getString(
                        KEY_SAVED_DESTINATION_NAME,
                        ""
                );

        String savedMode =
                prefs.getString(
                        KEY_SAVED_TRANSPORT_MODE,
                        "car"
                );

        if (savedMode != null && !savedMode.trim().isEmpty()) {
            selectedTransportMode = savedMode;
        }


        if (destination.isEmpty()) {
            return;
        }


        try {

            String[] values =
                    destination.split(",");


            if (values.length < 2) {
                return;
            }


            selectedDestination =
                    new GeoPoint(
                            Double.parseDouble(
                                    values[0]
                            ),
                            Double.parseDouble(
                                    values[1]
                            )
                    );


            selectedDestinationName =
                    name;


        } catch (Exception ignored) {
        }
    }


    private boolean loadSavedRouteForDestination() {

        if (selectedDestination == null) {
            return false;
        }


        SharedPreferences prefs =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );


        String savedDestination =
                prefs.getString(
                        KEY_SAVED_DESTINATION,
                        ""
                );


        String savedRoute =
                prefs.getString(
                        KEY_SAVED_ROUTE,
                        ""
                );


        if (
                savedDestination.isEmpty()
                        ||
                        savedRoute.isEmpty()
        ) {

            return false;
        }


        try {

            String[] destinationParts =
                    savedDestination.split(",");


            if (destinationParts.length < 2) {
                return false;
            }


            GeoPoint savedPoint =
                    new GeoPoint(
                            Double.parseDouble(
                                    destinationParts[0]
                            ),
                            Double.parseDouble(
                                    destinationParts[1]
                            )
                    );


            double distance =
                    selectedDestination
                            .distanceToAsDouble(
                                    savedPoint
                            );


            if (distance > 100) {
                return false;
            }


            String[] routePoints =
                    savedRoute.split(";");


            ArrayList<GeoPoint> points =
                    new ArrayList<>();


            for (
                    String point :
                    routePoints
            ) {

                if (point.trim().isEmpty()) {
                    continue;
                }


                String[] values =
                        point.split(",");


                if (values.length < 2) {
                    continue;
                }


                points.add(
                        new GeoPoint(
                                Double.parseDouble(
                                        values[0]
                                ),
                                Double.parseDouble(
                                        values[1]
                                )
                        )
                );
            }


            if (points.isEmpty()) {
                return false;
            }


            currentRoutePoints.clear();
            currentRoutePoints.addAll(points);

            // Rebuild distance/time for a saved offline route instead of
            // displaying a generic "Saved route" time.
            selectedRouteDistanceMeters =
                    calculatePolylineDistance(points);

            selectedRouteDurationSeconds =
                    selectedRouteDistanceMeters / getDefaultSpeed();

            drawRoute(
                    points
            );


            layoutDirections.setVisibility(
                    View.VISIBLE
            );


            btnStartNavigation.setVisibility(
                    View.VISIBLE
            );


            tvTravelInfo.setText(
                    formatDistance(selectedRouteDistanceMeters)
                            + " · "
                            + formatDuration(selectedRouteDurationSeconds)
            );


            tvTravelMode.setText(
                    getModeEmoji(selectedTransportMode)
                            + " "
                            + getModeLabel(selectedTransportMode)
            );


            tvRouteType.setText(
                    "Offline"
            );


            tvRouteStatus.setText(
                    "Saved route loaded offline."
            );


            return true;


        } catch (Exception e) {

            return false;
        }
    }


    private void showOfflineRouteMessage() {
        showOfflineRouteMessage(
                "No local road graph is available for offline routing."
        );
    }


    private void showOfflineRouteMessage(
            String detail
    ) {

        String safeDetail =
                detail == null || detail.trim().isEmpty()
                        ? "No local road graph is available for offline routing."
                        : detail;

        new AlertDialog.Builder(this)
                .setTitle(
                        "Offline routing"
                )
                .setMessage(
                        safeDetail
                                + "\n\n"
                                + "GNSS is working, but GNSS only gives your position. SheShield also needs a local road-routing graph to calculate a route without internet."
                )
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }


    // =========================================================
    // SOS
    // =========================================================

    private void confirmSOS() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Send Safety Alert?"
                )
                .setMessage(
                        "This will mark your current location as an active safety alert for SheShield users."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Mark Alert",
                        (dialog, which) ->
                                createOwnSOS()
                )
                .show();
    }


    private void createOwnSOS() {

        GeoPoint location =
                getBestAvailableLocation();


        if (location == null) {

            Toast.makeText(
                    this,
                    "Waiting for your GPS location...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String userId =
                getCurrentUserId();


        if (userId == null) {

            addLocalIncident(
                    "My Safety Alert",
                    "Active safety alert marked at this location.",
                    location,
                    true
            );


            Toast.makeText(
                    this,
                    "Safety alert marked on your map.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        if (!isNetworkAvailable()) {

            addLocalIncident(
                    "My Safety Alert",
                    "Active safety alert marked at this location.",
                    location,
                    true
            );


            Toast.makeText(
                    this,
                    "Offline: alert is marked locally.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        if (firestore == null) {

            addLocalIncident(
                    "My Safety Alert",
                    "Active safety alert marked at this location.",
                    location,
                    true
            );


            Toast.makeText(
                    this,
                    "Alert marked locally.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "userId",
                userId
        );


        data.put(
                "type",
                "SOS"
        );


        data.put(
                "title",
                "My Safety Alert"
        );


        data.put(
                "description",
                "Active safety alert marked at this location."
        );


        data.put(
                "latitude",
                location.getLatitude()
        );


        data.put(
                "longitude",
                location.getLongitude()
        );


        data.put(
                "radius",
                SOS_RADIUS_METERS
        );


        data.put(
                "active",
                true
        );


        data.put(
                "createdAt",
                System.currentTimeMillis()
        );


        firestore.collection(
                        "safety_incidents"
                )
                .add(data)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    this,
                                    "Safety alert shared.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            addLocalIncident(
                                    "My Safety Alert",
                                    "Active safety alert marked at this location.",
                                    location,
                                    true
                            );


                            Toast.makeText(
                                    this,
                                    "Alert marked locally. Sharing failed.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    private void addLocalIncident(
            String title,
            String description,
            GeoPoint location,
            boolean own
    ) {

        String localId =
                "local_"
                        + System.currentTimeMillis();


        addIncidentOverlay(
                localId,
                title,
                description,
                location.getLatitude(),
                location.getLongitude(),
                SOS_RADIUS_METERS,
                own
        );
    }


    // =========================================================
    // FIREBASE INCIDENTS
    // =========================================================

    private void setupFirebaseIncidents() {

        try {

            firestore =
                    FirebaseFirestore.getInstance();


            firebaseAuth =
                    FirebaseAuth.getInstance();


            listenForIncidents();


        } catch (Exception e) {

            firestore = null;

            firebaseAuth = null;
        }
    }


    private String getCurrentUserId() {

        if (
                firebaseAuth == null
                        ||
                        firebaseAuth.getCurrentUser() == null
        ) {

            return null;
        }


        return firebaseAuth
                .getCurrentUser()
                .getUid();
    }


    private void listenForIncidents() {

        if (firestore == null) {
            return;
        }


        if (incidentListener != null) {

            incidentListener.remove();
        }


        incidentListener =
                firestore.collection(
                                "safety_incidents"
                        )
                        .whereEqualTo(
                                "active",
                                true
                        )
                        .orderBy(
                                "createdAt",
                                Query.Direction.DESCENDING
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null
                                                    ||
                                                    snapshot == null
                                    ) {

                                        return;
                                    }


                                    for (
                                            DocumentChange change :
                                            snapshot.getDocumentChanges()
                                    ) {

                                        String id =
                                                change.getDocument()
                                                        .getId();


                                        if (
                                                change.getType()
                                                        ==
                                                        DocumentChange.Type.ADDED
                                        ) {

                                            addFirestoreIncident(
                                                    id,
                                                    change.getDocument()
                                            );


                                        } else if (
                                                change.getType()
                                                        ==
                                                        DocumentChange.Type.MODIFIED
                                        ) {

                                            removeIncidentOverlay(
                                                    id
                                            );


                                            addFirestoreIncident(
                                                    id,
                                                    change.getDocument()
                                            );


                                        } else if (
                                                change.getType()
                                                        ==
                                                        DocumentChange.Type.REMOVED
                                        ) {

                                            removeIncidentOverlay(
                                                    id
                                            );
                                        }
                                    }
                                }
                        );
    }


    private void addFirestoreIncident(
            String id,
            DocumentSnapshot document
    ) {

        Double latitude =
                document.getDouble(
                        "latitude"
                );


        Double longitude =
                document.getDouble(
                        "longitude"
                );


        if (
                latitude == null
                        ||
                        longitude == null
        ) {

            return;
        }


        String title =
                document.getString(
                        "title"
                );


        String description =
                document.getString(
                        "description"
                );


        String userId =
                document.getString(
                        "userId"
                );


        Double radius =
                document.getDouble(
                        "radius"
                );


        if (title == null) {

            title =
                    "Reported Safety Incident";
        }


        if (description == null) {

            description =
                    "A safety incident was reported here.";
        }


        if (
                radius == null
                        ||
                        radius <= 0
        ) {

            radius =
                    SOS_RADIUS_METERS;
        }


        boolean own =
                userId != null
                        &&
                        userId.equals(
                                getCurrentUserId()
                        );


        addIncidentOverlay(
                id,
                title,
                description,
                latitude,
                longitude,
                radius,
                own
        );
    }


    // =========================================================
    // INCIDENT OVERLAY
    // =========================================================

    private static class IncidentOverlay {

        Polygon zone;

        Marker marker;

        GeoPoint point;

        boolean own;

        String title;

        String description;

        double radius;


        IncidentOverlay(
                Polygon zone,
                Marker marker,
                GeoPoint point,
                boolean own,
                String title,
                String description,
                double radius
        ) {

            this.zone =
                    zone;

            this.marker =
                    marker;

            this.point =
                    point;

            this.own =
                    own;

            this.title =
                    title;

            this.description =
                    description;

            this.radius =
                    radius;
        }


        double radiusMeters() {

            return radius;
        }
    }


    private void addIncidentOverlay(
            String id,
            String title,
            String description,
            double latitude,
            double longitude,
            double radius,
            boolean own
    ) {

        removeIncidentOverlay(
                id
        );


        GeoPoint point =
                new GeoPoint(
                        latitude,
                        longitude
                );


        final String finalTitle =
                (
                        title == null
                                ||
                                title.trim().isEmpty()
                )
                        ?
                        "Reported Safety Incident"
                        :
                        title;


        final String finalDescription =
                (
                        description == null
                                ||
                                description.trim().isEmpty()
                )
                        ?
                        "A safety incident was reported here."
                        :
                        description;


        final boolean finalOwn =
                own;


        final double finalRadius =
                radius > 0
                        ?
                        radius
                        :
                        SOS_RADIUS_METERS;


        Marker marker =
                new Marker(
                        mapView
                );


        marker.setPosition(
                point
        );


        marker.setTitle(
                finalOwn
                        ?
                        "My Safety Alert"
                        :
                        "Reported Safety Incident"
        );


        marker.setSnippet(
                finalDescription
        );


        marker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
        );


        Polygon zone =
                new Polygon();


        zone.setPoints(
                Polygon.pointsAsCircle(
                        point,
                        finalRadius
                )
        );


        if (finalOwn) {

            zone.setFillColor(
                    Color.argb(
                            55,
                            244,
                            67,
                            54
                    )
            );


            zone.setStrokeColor(
                    Color.rgb(
                            211,
                            47,
                            47
                    )
            );


        } else {

            zone.setFillColor(
                    Color.argb(
                            45,
                            255,
                            152,
                            0
                    )
            );


            zone.setStrokeColor(
                    Color.rgb(
                            245,
                            124,
                            0
                    )
            );
        }


        zone.setStrokeWidth(
                3.0f
        );


        marker.setOnMarkerClickListener(
                (m, map) -> {

                    showIncidentDetails(
                            id,
                            finalTitle,
                            finalDescription,
                            point,
                            finalOwn
                    );


                    return true;
                }
        );


        mapView.getOverlays()
                .add(
                        zone
                );


        mapView.getOverlays()
                .add(
                        marker
                );


        incidentOverlays.put(
                id,
                new IncidentOverlay(
                        zone,
                        marker,
                        point,
                        finalOwn,
                        finalTitle,
                        finalDescription,
                        finalRadius
                )
        );


        mapView.invalidate();
    }


    private void removeIncidentOverlay(
            String id
    ) {

        IncidentOverlay overlay =
                incidentOverlays.remove(
                        id
                );


        if (overlay == null) {
            return;
        }


        mapView.getOverlays()
                .remove(
                        overlay.zone
                );


        mapView.getOverlays()
                .remove(
                        overlay.marker
                );


        mapView.invalidate();
    }


    // =========================================================
    // TAP INSIDE INCIDENT AREA
    // =========================================================

    @Override
    public boolean singleTapConfirmedHelper(
            GeoPoint point
    ) {

        for (
                Map.Entry<String, IncidentOverlay> entry :
                incidentOverlays.entrySet()
        ) {

            IncidentOverlay incident =
                    entry.getValue();


            double distance =
                    point.distanceToAsDouble(
                            incident.point
                    );


            if (
                    distance <=
                            incident.radiusMeters()
            ) {

                showIncidentDetails(
                        entry.getKey(),
                        incident.title,
                        incident.description,
                        incident.point,
                        incident.own
                );


                return true;
            }
        }


        return false;
    }


    @Override
    public boolean longPressHelper(
            GeoPoint point
    ) {

        return false;
    }


    // =========================================================
    // INCIDENT DETAILS
    // =========================================================

    // =========================================================
    // INCIDENT DETAILS BOTTOM SHEET
    // =========================================================

    private void showIncidentDetails(
            String id,
            String title,
            String description,
            GeoPoint point,
            boolean own
    ) {

        BottomSheetDialog bottomSheetDialog =
                new BottomSheetDialog(this);

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_incident_details,
                        null
                );

        bottomSheetDialog.setContentView(dialogView);

        TextView tvIncidentTitle =
                dialogView.findViewById(R.id.tvIncidentTitle);

        TextView tvIncidentDescription =
                dialogView.findViewById(R.id.tvIncidentDescription);

        TextView tvIncidentTime =
                dialogView.findViewById(R.id.tvIncidentTime);

        MaterialButton btnNavigateAround =
                dialogView.findViewById(R.id.btnNavigateAround);

        if (tvIncidentTitle != null) {
            tvIncidentTitle.setText(
                    own
                            ? "My Safety Alert"
                            : (title != null && !title.trim().isEmpty()
                               ? title
                               : "Reported Safety Incident")
            );
        }

        if (tvIncidentDescription != null) {
            tvIncidentDescription.setText(
                    description != null && !description.trim().isEmpty()
                            ? description
                            : (own
                               ? "Active safety alert marked at this location."
                               : "A safety incident was reported here.")
            );
        }

        if (tvIncidentTime != null) {
            tvIncidentTime.setText(
                    own
                            ? "Your active safety alert is marked here."
                            : "Reported safety incident active near this area."
            );
        }

        if (btnNavigateAround != null) {
            btnNavigateAround.setText(
                    "Navigate Safely Around Area"
            );

            // SheShield pink / red / white theme
            styleSafetyActionButton(
                    btnNavigateAround,
                    Color.rgb(251, 87, 146),
                    Color.WHITE,
                    Color.rgb(251, 87, 146)
            );

            btnNavigateAround.setOnClickListener(v -> {

                bottomSheetDialog.dismiss();

                if (selectedDestination != null) {
                    // Recalculate the route while considering marked incident zones.
                    calculateRoute(true);
                } else {
                    Toast.makeText(
                            OfflineMapActivity.this,
                            "Select a destination first, then navigation can be started.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }

        // Add the two extra actions in Java so no XML layout change is required.
        if (dialogView instanceof android.view.ViewGroup) {

            android.view.ViewGroup root =
                    (android.view.ViewGroup) dialogView;

            LinearLayout actionLayout =
                    new LinearLayout(this);

            actionLayout.setOrientation(
                    LinearLayout.VERTICAL
            );

            actionLayout.setPadding(
                    dpToPx(18),
                    dpToPx(4),
                    dpToPx(18),
                    dpToPx(18)
            );

            MaterialButton btnReportIncident =
                    new MaterialButton(this);

            btnReportIncident.setText(
                    "Report Incident"
            );

            btnReportIncident.setTextSize(14);
            btnReportIncident.setAllCaps(false);

            // Red/pink emergency-action style
            styleSafetyActionButton(
                    btnReportIncident,
                    Color.rgb(229, 57, 53),
                    Color.WHITE,
                    Color.rgb(229, 57, 53)
            );

            btnReportIncident.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                // This uses your existing report flow for now.
                // Later, replace this one line with:
                // startActivity(new Intent(this, ReportIncidentActivity.class));
                showReportIncidentDialog(point);
            });

            actionLayout.addView(
                    btnReportIncident,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(52)
                    )
            );

            if (own) {

                MaterialButton btnRemoveAlert =
                        new MaterialButton(this);

                btnRemoveAlert.setText(
                        "Remove My Safety Alert"
                );

                btnRemoveAlert.setTextSize(14);
                btnRemoveAlert.setAllCaps(false);

                // White button with pink/red border — no grey theme
                styleSafetyActionButton(
                        btnRemoveAlert,
                        Color.WHITE,
                        Color.rgb(214, 39, 93),
                        Color.rgb(214, 39, 93)
                );

                LinearLayout.LayoutParams removeParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dpToPx(52)
                        );

                removeParams.topMargin = dpToPx(8);

                btnRemoveAlert.setOnClickListener(v -> {
                    bottomSheetDialog.dismiss();
                    confirmDeleteOwnIncident(id);
                });

                actionLayout.addView(
                        btnRemoveAlert,
                        removeParams
                );
            }

            // Put the action group after the existing content.
            root.addView(actionLayout);
        }

        bottomSheetDialog.show();
    }


    /**
     * Applies the SheShield pink / red / white button style used by
     * incident and safety-alert actions.
     */
    private void styleSafetyActionButton(
            MaterialButton button,
            int backgroundColor,
            int textColor,
            int strokeColor
    ) {

        if (button == null) {
            return;
        }

        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(textColor);
        button.setCornerRadius(dpToPx(14));
        button.setMinHeight(dpToPx(50));
        button.setPadding(
                dpToPx(16),
                0,
                dpToPx(16),
                0
        );

        button.setBackgroundTintList(
                ColorStateList.valueOf(backgroundColor)
        );

        button.setStrokeWidth(dpToPx(1));
        button.setStrokeColor(
                ColorStateList.valueOf(strokeColor)
        );

        // Clean white/pink/red ripple instead of the default grey ripple.
        button.setRippleColor(
                ColorStateList.valueOf(
                        Color.argb(45, 255, 255, 255)
                )
        );
    }


    // =========================================================
    // DP TO PX
    // =========================================================

    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp
                        *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }


    // =========================================================
    // OLD LOCAL REPORT DIALOG
    // =========================================================

    private void showReportIncidentDialog(
            GeoPoint point
    ) {

        LinearLayout layout =
                new LinearLayout(
                        this
                );


        layout.setOrientation(
                LinearLayout.VERTICAL
        );


        layout.setPadding(
                dpToPx(30),
                dpToPx(20),
                dpToPx(30),
                dpToPx(10)
        );


        TextView title =
                new TextView(
                        this
                );


        title.setText(
                "Report a Safety Incident"
        );


        title.setTextSize(
                20
        );


        title.setTextColor(
                Color.rgb(
                        26,
                        26,
                        46
                )
        );


        title.setTypeface(
                null,
                Typeface.BOLD
        );


        EditText description =
                new EditText(
                        this
                );


        description.setHint(
                "What happened here?"
        );


        description.setMinLines(
                3
        );


        description.setGravity(
                Gravity.TOP
        );


        layout.addView(
                title
        );


        layout.addView(
                description
        );


        new AlertDialog.Builder(
                this
        )
                .setView(
                        layout
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Submit Report",
                        (dialog, which) ->
                                submitIncidentReport(
                                        point,
                                        description
                                                .getText()
                                                .toString()
                                                .trim()
                                )
                )
                .show();
    }


    private void submitIncidentReport(
            GeoPoint point,
            String description
    ) {

        if (description.isEmpty()) {

            description =
                    "Safety incident reported at this location.";
        }


        String userId =
                getCurrentUserId();


        if (
                firestore == null
                        ||
                        userId == null
                        ||
                        !isNetworkAvailable()
        ) {

            addLocalIncident(
                    "Reported Safety Incident",
                    description,
                    point,
                    false
            );


            Toast.makeText(
                    this,
                    "Incident marked locally.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "userId",
                userId
        );


        data.put(
                "type",
                "REPORT"
        );


        data.put(
                "title",
                "Reported Safety Incident"
        );


        data.put(
                "description",
                description
        );


        data.put(
                "latitude",
                point.getLatitude()
        );


        data.put(
                "longitude",
                point.getLongitude()
        );


        data.put(
                "radius",
                SOS_RADIUS_METERS
        );


        data.put(
                "active",
                true
        );


        data.put(
                "createdAt",
                System.currentTimeMillis()
        );


        firestore.collection(
                        "safety_incidents"
                )
                .add(
                        data
                )
                .addOnSuccessListener(
                        documentReference ->

                                Toast.makeText(
                                        this,
                                        "Incident reported.",
                                        Toast.LENGTH_LONG
                                ).show()
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Could not submit report.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // =========================================================
    // DELETE OWN ALERT
    // =========================================================

    private void confirmDeleteOwnIncident(
            String incidentId
    ) {

        if (
                incidentId.startsWith(
                        "local_"
                )
        ) {

            removeIncidentOverlay(
                    incidentId
            );


            Toast.makeText(
                    this,
                    "Your local alert was removed.",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Remove your alert?"
                )
                .setMessage(
                        "This removes your own safety alert from the shared map."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Remove",
                        (dialog, which) ->
                                deleteOwnIncident(
                                        incidentId
                                )
                )
                .show();
    }


    private void deleteOwnIncident(
            String incidentId
    ) {

        if (firestore == null) {
            return;
        }


        String currentUserId =
                getCurrentUserId();


        if (currentUserId == null) {
            return;
        }


        firestore.collection(
                        "safety_incidents"
                )
                .document(
                        incidentId
                )
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {
                                return;
                            }


                            String owner =
                                    document.getString(
                                            "userId"
                                    );


                            if (
                                    !currentUserId.equals(
                                            owner
                                    )
                            ) {

                                Toast.makeText(
                                        this,
                                        "You can only remove your own alert.",
                                        Toast.LENGTH_LONG
                                ).show();


                                return;
                            }


                            firestore.collection(
                                            "safety_incidents"
                                    )
                                    .document(
                                            incidentId
                                    )
                                    .update(
                                            "active",
                                            false
                                    )
                                    .addOnSuccessListener(
                                            unused ->
                                                    Toast.makeText(
                                                            this,
                                                            "Your alert was removed.",
                                                            Toast.LENGTH_SHORT
                                                    ).show()
                                    )
                                    .addOnFailureListener(
                                            e ->
                                                    Toast.makeText(
                                                            this,
                                                            "Could not remove alert.",
                                                            Toast.LENGTH_LONG
                                                    ).show()
                                    );
                        }
                );
    }


    // =========================================================
    // NETWORK
    // =========================================================

    private boolean isNetworkAvailable() {

        ConnectivityManager manager =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE
                        );


        if (manager == null) {
            return false;
        }


        Network network =
                manager.getActiveNetwork();


        if (network == null) {
            return false;
        }


        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(
                        network
                );


        return capabilities != null
                &&
                capabilities.hasCapability(
                        NetworkCapabilities
                                .NET_CAPABILITY_INTERNET
                );
    }


    private void updateNetworkStatus() {

        boolean online = isNetworkAvailable();

        if (tvNetworkStatus != null) {
            if (online) {
                tvNetworkStatus.setText("● Online");
                tvNetworkStatus.setTextColor(
                        Color.rgb(46, 125, 50)
                );
            } else {
                tvNetworkStatus.setText("● Offline");
                tvNetworkStatus.setTextColor(
                        Color.rgb(117, 117, 117)
                );
            }
        }

        // Switch the map between live downloading and cache-only mode.
        // Cached OSMDroid tiles remain available when this is false.
        if (mapView != null) {
            mapView.setUseDataConnection(online);
            mapView.invalidate();
        }
    }


    // =========================================================
    // UI HELPERS
    // =========================================================

    private void resetRouteButtons() {

        layoutRouteOptions.setVisibility(
                View.GONE
        );


        layoutDirections.setVisibility(
                View.GONE
        );


        btnStartNavigation.setVisibility(
                View.GONE
        );
    }


    private String formatDistance(
            double meters
    ) {

        if (meters < 1000) {

            return Math.round(
                    meters
            )
                    + " m";
        }


        return String.format(
                Locale.getDefault(),
                "%.1f km",
                meters / 1000.0
        );
    }


    private String formatDuration(
            double seconds
    ) {

        long minutes =
                Math.round(
                        seconds / 60.0
                );


        if (minutes < 1) {

            return "< 1 min";
        }


        if (minutes < 60) {

            return minutes
                    + " min";
        }


        long hours =
                minutes / 60;


        long remaining =
                minutes % 60;


        if (remaining == 0) {

            return hours
                    + " hr";
        }


        return hours
                + " hr "
                + remaining
                + " min";
    }


    // =========================================================
    // PERMISSION
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        if (
                requestCode ==
                        LOCATION_PERMISSION_REQUEST
        ) {

            if (
                    grantResults.length > 0
                            &&
                            grantResults[0] ==
                                    PackageManager.PERMISSION_GRANTED
            ) {

                enableLocation();


            } else {

                Toast.makeText(
                        this,
                        "Location permission is required for live navigation.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }


    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (mapView != null) {

            mapView.onResume();
        }


        updateNetworkStatus();
    }


    @Override
    protected void onPause() {

        if (mapView != null) {

            mapView.onPause();
        }


        super.onPause();
    }


    @Override
    protected void onDestroy() {

        navigationStarted =
                false;


        if (navigationUpdater != null) {

            mainHandler.removeCallbacks(
                    navigationUpdater
            );


            navigationUpdater =
                    null;
        }


        if (searchRunnable != null) {

            searchHandler.removeCallbacks(
                    searchRunnable
            );


            searchRunnable =
                    null;
        }

        stopGnssLocation();
        latestGnssLocation = null;

        fusedLocationCallback = null;
        fusedLocationClient = null;

        if (offlineHopper != null) {
            try {
                offlineHopper.close();
            } catch (Exception ignored) {
            }
            offlineHopper = null;
        }

        offlineRoutingExecutor.shutdownNow();
        searchExecutor.shutdownNow();


        if (incidentListener != null) {

            incidentListener.remove();

            incidentListener =
                    null;
        }

        if (offlineMapTileProvider != null) {

            try {
                offlineMapTileProvider.detach();
            } catch (Exception ignored) {
            }

            offlineMapTileProvider =
                    null;
        }

        if (offlineMapExecutor != null) {
            try {
                offlineMapExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
            offlineMapExecutor = null;
        }

        if (offlineMapReceiver != null) {

            try {
                offlineMapReceiver.destroy();
            } catch (Exception ignored) {
            }

            offlineMapReceiver =
                    null;
        }

        if (mapView != null) {

            mapView.onDetach();
        }

        super.onDestroy();
    }


    // =========================================================
    // SEARCH SUGGESTION ADAPTER
    // =========================================================

    private static class SearchSuggestionAdapter
            extends ArrayAdapter<PhotonPlace> {

        private final android.view.LayoutInflater inflater;

        SearchSuggestionAdapter(Context context) {

            super(
                    context,
                    android.R.layout.simple_list_item_2,
                    new ArrayList<PhotonPlace>()
            );

            inflater =
                    android.view.LayoutInflater.from(context);
        }

        @Override
        public View getView(
                int position,
                View convertView,
                android.view.ViewGroup parent
        ) {

            LinearLayout row;

            if (convertView instanceof LinearLayout) {

                row = (LinearLayout) convertView;

            } else {

                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.VERTICAL);

                int horizontal = Math.round(
                        16 * getContext()
                                .getResources()
                                .getDisplayMetrics()
                                .density
                );

                int vertical = Math.round(
                        10 * getContext()
                                .getResources()
                                .getDisplayMetrics()
                                .density
                );

                row.setPadding(
                        horizontal,
                        vertical,
                        horizontal,
                        vertical
                );

                TextView title = new TextView(getContext());
                title.setTag("title");
                title.setTextSize(16);
                title.setTypeface(null, Typeface.BOLD);
                row.addView(
                        title,
                        new LinearLayout.LayoutParams(-1, -2)
                );

                TextView address = new TextView(getContext());
                address.setTag("address");
                address.setTextSize(13);
                address.setMaxLines(2);
                row.addView(
                        address,
                        new LinearLayout.LayoutParams(-1, -2)
                );
            }

            PhotonPlace place = getItem(position);

            TextView title = row.findViewWithTag("title");
            TextView address = row.findViewWithTag("address");

            if (place != null) {

                title.setText(
                        place.name == null || place.name.trim().isEmpty()
                                ? "Selected location"
                                : place.name
                );

                if (place.address != null
                        && !place.address.trim().isEmpty()) {

                    address.setText(place.address);
                    address.setVisibility(View.VISIBLE);

                } else {

                    address.setText("");
                    address.setVisibility(View.GONE);
                }

            } else {

                title.setText("");
                address.setText("");
                address.setVisibility(View.GONE);
            }

            return row;
        }

        @Override
        public android.widget.Filter getFilter() {

            return new android.widget.Filter() {

                @Override
                protected android.widget.Filter.FilterResults performFiltering(
                        CharSequence constraint
                ) {

                    android.widget.Filter.FilterResults results =
                            new android.widget.Filter.FilterResults();

                    ArrayList<PhotonPlace> values =
                            new ArrayList<>();

                    // The Activity performs the real live search.
                    // Return the current manually populated results.
                    for (int i = 0; i < getCount(); i++) {

                        PhotonPlace place = getItem(i);

                        if (place != null) {
                            values.add(place);
                        }
                    }

                    results.values = values;
                    results.count = values.size();

                    return results;
                }

                @Override
                protected void publishResults(
                        CharSequence constraint,
                        android.widget.Filter.FilterResults results
                ) {

                    // Do not let AutoCompleteTextView replace the
                    // Activity-managed Photon/offline result list.
                    notifyDataSetChanged();
                }
            };
        }
    }


    // =========================================================
    // PHOTON PLACE
    // =========================================================

    private static class PhotonPlace {

        String name;

        String address;

        double latitude;

        double longitude;


        PhotonPlace(
                String name,
                String address,
                double latitude,
                double longitude
        ) {

            this.name =
                    name;


            this.address =
                    address;


            this.latitude =
                    latitude;


            this.longitude =
                    longitude;
        }


        String getDisplayText() {

            if (
                    address == null
                            ||
                            address.trim().isEmpty()
            ) {

                return name;
            }


            if (
                    name == null
                            ||
                            name.trim().isEmpty()
            ) {

                return address;
            }


            /*
             * One-line display because the current
             * Android dropdown layout is a one-line
             * layout.
             */
            return name
                    + " • "
                    + address;
        }
    }


    // =========================================================
    // LOCAL PLACE
    // =========================================================

    private static class LocalPlace {

        String name;

        String address;

        double latitude;

        double longitude;


        LocalPlace(
                String name,
                String address,
                double latitude,
                double longitude
        ) {

            this.name =
                    name;


            this.address =
                    address;


            this.latitude =
                    latitude;


            this.longitude =
                    longitude;
        }
    }
}

