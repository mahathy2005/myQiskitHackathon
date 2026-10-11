package com.sheshield.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SafeZonesActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final float SEARCH_RADIUS_KM = 5.0f;

    private static final int MAX_RESULTS = 20;

    private static final long LOCATION_UPDATE_INTERVAL = 2000L;

    private static final float LOCATION_MIN_DISTANCE = 5.0f;

    private static final long LOCATION_TIMEOUT = 15000L;

    private LocationManager locationManager;

    private Location currentLocation;

    private TextView tvCurrentLocation;
    private TextView tvResultCount;

    private ProgressBar progressBar;

    private RecyclerView rvSafeZones;

    private MaterialButton btnRefreshLocation;

    private Chip chipAll;
    private Chip chipPolice;
    private Chip chipHospitals;
    private Chip chipCrowded;

    private SafeZonesAdapter adapter;

    private final List<SafePlace> allPlaces = new ArrayList<>();

    private String currentFilter = "ALL";

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean locationReceived = false;

    // ============================================================
    // LOCATION LISTENER
    // ============================================================

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        @NonNull Location location) {

                    if (location == null) {
                        return;
                    }

                    locationReceived = true;

                    handler.removeCallbacks(locationTimeoutRunnable);

                    onLocationFound(location);
                }

                @Override
                public void onProviderEnabled(
                        @NonNull String provider) {
                }

                @Override
                public void onProviderDisabled(
                        @NonNull String provider) {
                }

                @Override
                public void onStatusChanged(
                        String provider,
                        int status,
                        Bundle extras) {
                }
            };

    // ============================================================
    // LOCATION TIMEOUT
    // ============================================================

    private final Runnable locationTimeoutRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (locationReceived) {
                        return;
                    }

                    // One last attempt to get ANY cached location
                    Location fallback =
                            getBestLastKnownLocation();

                    if (fallback != null) {

                        locationReceived = true;

                        onLocationFound(fallback);

                        return;
                    }

                    stopLocationUpdates();

                    hideLoading();

                    if (tvCurrentLocation != null) {
                        tvCurrentLocation.setText(
                                "Location unavailable"
                        );
                    }

                    if (tvResultCount != null) {
                        tvResultCount.setText(
                                "Enable Location and try Refresh"
                        );
                    }

                    Toast.makeText(
                            SafeZonesActivity.this,
                            "Could not get your location. Please enable Location and try again.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            };

    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_safe_zones);

        tvCurrentLocation =
                findViewById(R.id.tvCurrentLocation);

        tvResultCount =
                findViewById(R.id.tvResultCount);

        progressBar =
                findViewById(R.id.progressBar);

        rvSafeZones =
                findViewById(R.id.rvSafeZones);

        btnRefreshLocation =
                findViewById(R.id.btnRefreshLocation);

        chipAll =
                findViewById(R.id.chipAll);

        chipPolice =
                findViewById(R.id.chipPolice);

        chipHospitals =
                findViewById(R.id.chipHospitals);

        chipCrowded =
                findViewById(R.id.chipCrowded);

        rvSafeZones.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvSafeZones.setHasFixedSize(false);

        adapter =
                new SafeZonesAdapter(
                        new ArrayList<>(),

                        new SafeZonesAdapter.OnPlaceClickListener() {

                            @Override
                            public void onNavigate(
                                    SafePlace place) {

                                openOfflineLocation(place);
                            }

                            @Override
                            public void onCall(
                                    SafePlace place) {

                                callPlace(place);
                            }
                        }
                );

        rvSafeZones.setAdapter(adapter);

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );

        setupFilters();

        if (btnRefreshLocation != null) {

            btnRefreshLocation.setOnClickListener(
                    v -> {

                        currentLocation = null;

                        locationReceived = false;

                        getCurrentLocation();
                    }
            );
        }

        // Load local JSON first.
        // NO INTERNET REQUIRED.
        loadOfflineSafeZones();

        checkLocationPermission();
    }

    // ============================================================
    // LOAD OFFLINE SAFE ZONES
    // ============================================================

    private void loadOfflineSafeZones() {

        allPlaces.clear();

        try {

            InputStream inputStream =
                    getAssets().open(
                            "safe_zones.json"
                    );

            String json =
                    readAssetFile(inputStream);

            if (json == null ||
                    json.trim().isEmpty()) {

                hideLoading();

                Toast.makeText(
                        this,
                        "safe_zones.json is empty",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            JSONObject root =
                    new JSONObject(json);

            JSONArray places =
                    root.optJSONArray(
                            "safe_zones"
                    );

            if (places == null) {

                hideLoading();

                Toast.makeText(
                        this,
                        "safe_zones.json has no safe_zones array",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            for (int i = 0;
                 i < places.length();
                 i++) {

                JSONObject object =
                        places.optJSONObject(i);

                if (object == null) {
                    continue;
                }

                String id =
                        object.optString(
                                "id",
                                String.valueOf(i + 1)
                        );

                String name =
                        object.optString(
                                "name",
                                "Safe Place"
                        );

                String category =
                        object.optString(
                                "category",
                                "SAFE PLACE"
                        );

                String address =
                        object.optString(
                                "address",
                                "Address unavailable"
                        );

                String phone =
                        object.optString(
                                "phone",
                                ""
                        );

                double latitude =
                        object.optDouble(
                                "latitude",
                                Double.NaN
                        );

                double longitude =
                        object.optDouble(
                                "longitude",
                                Double.NaN
                        );

                if (Double.isNaN(latitude) ||
                        Double.isNaN(longitude)) {

                    continue;
                }

                if (latitude == 0 ||
                        longitude == 0) {

                    continue;
                }

                SafePlace place =
                        new SafePlace(
                                id,
                                name,
                                category,
                                address,
                                phone,
                                latitude,
                                longitude
                        );

                allPlaces.add(place);
            }

            if (tvResultCount != null) {

                tvResultCount.setText(
                        allPlaces.size()
                                + " offline places available"
                );
            }

        } catch (Exception e) {

            hideLoading();

            if (tvResultCount != null) {

                tvResultCount.setText(
                        "Could not load offline places"
                );
            }

            Toast.makeText(
                    this,
                    "Error loading safe_zones.json",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ============================================================
    // READ JSON
    // ============================================================

    private String readAssetFile(
            InputStream inputStream)
            throws Exception {

        StringBuilder builder =
                new StringBuilder();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while (
                (line = reader.readLine())
                        != null
        ) {

            builder.append(line);
        }

        reader.close();

        inputStream.close();

        return builder.toString();
    }

    // ============================================================
    // FILTERS
    // ============================================================

    private void setupFilters() {

        if (chipAll != null) {

            chipAll.setOnClickListener(
                    v -> {

                        currentFilter = "ALL";

                        loadFilteredPlaces();
                    }
            );
        }

        if (chipPolice != null) {

            chipPolice.setOnClickListener(
                    v -> {

                        currentFilter = "POLICE";

                        loadFilteredPlaces();
                    }
            );
        }

        if (chipHospitals != null) {

            chipHospitals.setOnClickListener(
                    v -> {

                        currentFilter = "HOSPITAL";

                        loadFilteredPlaces();
                    }
            );
        }

        if (chipCrowded != null) {

            chipCrowded.setOnClickListener(
                    v -> {

                        currentFilter = "CROWDED";

                        loadFilteredPlaces();
                    }
            );
        }
    }

    // ============================================================
    // PERMISSION
    // ============================================================

    private void checkLocationPermission() {

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {

            ActivityCompat.requestPermissions(

                    this,

                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },

                    LOCATION_PERMISSION_REQUEST_CODE
            );

        } else {

            getCurrentLocation();
        }
    }

    // ============================================================
    // MAIN LOCATION METHOD
    // ============================================================

    private void getCurrentLocation() {

        locationReceived = false;

        handler.removeCallbacks(
                locationTimeoutRunnable
        );

        showLoading();

        if (tvCurrentLocation != null) {

            tvCurrentLocation.setText(
                    "Finding your location..."
            );
        }

        if (locationManager == null) {

            hideLoading();

            if (tvCurrentLocation != null) {

                tvCurrentLocation.setText(
                        "Location service unavailable"
                );
            }

            return;
        }

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {

            hideLoading();

            checkLocationPermission();

            return;
        }

        // ========================================================
        // STEP 1
        // USE ANY EXISTING LOCATION FIRST
        // ========================================================

        Location lastLocation =
                getBestLastKnownLocation();

        if (lastLocation != null) {

            locationReceived = true;

            onLocationFound(lastLocation);

            // Continue requesting a newer location silently.
            requestFreshLocation();

            return;
        }

        // ========================================================
        // STEP 2
        // REQUEST GPS + NETWORK
        // ========================================================

        requestFreshLocation();
    }

    // ============================================================
    // REQUEST GPS + NETWORK LOCATION
    // ============================================================

    private void requestFreshLocation() {

        if (locationManager == null) {
            showLocationError();
            return;
        }

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            showLocationError();
            return;
        }

        stopLocationUpdates();

        boolean requested = false;

        // ========================================================
        // GPS
        // ========================================================

        try {

            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                locationManager.requestLocationUpdates(

                        LocationManager.GPS_PROVIDER,

                        LOCATION_UPDATE_INTERVAL,

                        LOCATION_MIN_DISTANCE,

                        locationListener
                );

                requested = true;
            }

        } catch (Exception ignored) {
        }

        // ========================================================
        // NETWORK / WIFI / CELL TOWER
        // ========================================================

        try {

            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {

                locationManager.requestLocationUpdates(

                        LocationManager.NETWORK_PROVIDER,

                        LOCATION_UPDATE_INTERVAL,

                        LOCATION_MIN_DISTANCE,

                        locationListener
                );

                requested = true;
            }

        } catch (Exception ignored) {
        }

        // ========================================================
        // PASSIVE PROVIDER
        // ========================================================

        try {

            if (locationManager.isProviderEnabled(
                    LocationManager.PASSIVE_PROVIDER)) {

                locationManager.requestLocationUpdates(

                        LocationManager.PASSIVE_PROVIDER,

                        LOCATION_UPDATE_INTERVAL,

                        LOCATION_MIN_DISTANCE,

                        locationListener
                );

                requested = true;
            }

        } catch (Exception ignored) {
        }

        if (!requested) {

            showLocationError();

            return;
        }

        if (tvCurrentLocation != null) {

            tvCurrentLocation.setText(
                    "Calculating location..."
            );
        }

        // ========================================================
        // DON'T WAIT FOREVER
        // ========================================================

        handler.removeCallbacks(
                locationTimeoutRunnable
        );

        handler.postDelayed(
                locationTimeoutRunnable,
                LOCATION_TIMEOUT
        );
    }

    // ============================================================
    // GET BEST CACHED LOCATION
    // ============================================================

    private Location getBestLastKnownLocation() {

        if (locationManager == null) {
            return null;
        }

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            return null;
        }

        Location best = null;

        try {

            // GPS cached location
            Location gps =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (gps != null) {
                best = gps;
            }

        } catch (Exception ignored) {
        }

        try {

            // Network/Wi-Fi/Cell cached location
            Location network =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );

            if (network != null) {

                if (best == null ||
                        network.getTime() > best.getTime()) {

                    best = network;
                }
            }

        } catch (Exception ignored) {
        }

        try {

            // Passive cached location
            Location passive =
                    locationManager.getLastKnownLocation(
                            LocationManager.PASSIVE_PROVIDER
                    );

            if (passive != null) {

                if (best == null ||
                        passive.getTime() > best.getTime()) {

                    best = passive;
                }
            }

        } catch (Exception ignored) {
        }

        return best;
    }

    // ============================================================
    // LOCATION FOUND
    // ============================================================

    private void onLocationFound(
            Location location) {

        if (location == null) {
            return;
        }

        locationReceived = true;

        currentLocation = location;

        handler.removeCallbacks(
                locationTimeoutRunnable
        );

        hideLoading();

        stopLocationUpdates();

        if (tvCurrentLocation != null) {

            String source = "Location";

            if (LocationManager.GPS_PROVIDER.equals(
                    location.getProvider())) {

                source = "GPS";
            } else if (
                    LocationManager.NETWORK_PROVIDER.equals(
                            location.getProvider())) {

                source = "Network";
            } else if (
                    LocationManager.PASSIVE_PROVIDER.equals(
                            location.getProvider())) {

                source = "Cached";
            }

            tvCurrentLocation.setText(

                    String.format(
                            Locale.getDefault(),

                            "%s • %.5f, %.5f",

                            source,

                            location.getLatitude(),

                            location.getLongitude()
                    )
            );
        }

        loadFilteredPlaces();
    }

    // ============================================================
    // FILTER + DISTANCE CALCULATION
    // ============================================================

    private void loadFilteredPlaces() {

        if (adapter == null) {
            return;
        }

        if (currentLocation == null) {

            if (tvResultCount != null) {

                tvResultCount.setText(
                        "Waiting for location..."
                );
            }

            return;
        }

        List<SafePlace> nearbyPlaces =
                new ArrayList<>();

        for (SafePlace place : allPlaces) {

            if (place == null) {
                continue;
            }

            float[] distance =
                    new float[1];

            Location.distanceBetween(

                    currentLocation.getLatitude(),

                    currentLocation.getLongitude(),

                    place.latitude,

                    place.longitude,

                    distance
            );

            place.distanceKm =
                    distance[0] / 1000.0;

            if (place.distanceKm >
                    SEARCH_RADIUS_KM) {

                continue;
            }

            if (matchesCurrentFilter(place)) {

                nearbyPlaces.add(place);
            }
        }

        Collections.sort(

                nearbyPlaces,

                Comparator.comparingDouble(
                        place ->
                                place.distanceKm
                )
        );

        if (nearbyPlaces.size() >
                MAX_RESULTS) {

            nearbyPlaces =
                    new ArrayList<>(
                            nearbyPlaces.subList(
                                    0,
                                    MAX_RESULTS
                            )
                    );
        }

        adapter.updateList(
                nearbyPlaces
        );

        updateResultCount(
                nearbyPlaces.size()
        );
    }

    // ============================================================
    // FILTER MATCH
    // ============================================================

    private boolean matchesCurrentFilter(
            SafePlace place) {

        if (place == null) {
            return false;
        }

        if (currentFilter.equals("ALL")) {
            return true;
        }

        String category =
                place.category == null
                        ? ""
                        : place.category
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (currentFilter.equals("POLICE")) {

            return category.contains(
                    "POLICE"
            );
        }

        if (currentFilter.equals("HOSPITAL")) {

            return category.contains(
                    "HOSPITAL"
            )
                    ||
                    category.contains(
                            "CLINIC"
                    )
                    ||
                    category.contains(
                            "PHARMACY"
                    );
        }

        if (currentFilter.equals("CROWDED")) {

            return category.contains(
                    "CROWDED"
            )
                    ||
                    category.contains(
                            "BUS"
                    )
                    ||
                    category.contains(
                            "METRO"
                    )
                    ||
                    category.contains(
                            "MARKET"
                    )
                    ||
                    category.contains(
                            "MALL"
                    )
                    ||
                    category.contains(
                            "STATION"
                    );
        }

        return true;
    }

    // ============================================================
    // RESULT COUNT
    // ============================================================

    private void updateResultCount(
            int count) {

        if (tvResultCount == null) {
            return;
        }

        if (count == 0) {

            tvResultCount.setText(
                    "No safe places within 5 km"
            );

        } else if (count == 1) {

            tvResultCount.setText(
                    "1 safe place found nearby"
            );

        } else {

            tvResultCount.setText(
                    count +
                            " safe places found nearby"
            );
        }
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    private void openOfflineLocation(
            SafePlace place) {

        if (place == null) {
            return;
        }

        try {

            // ---------------------------------------------------------
            // OPEN SHE SHIELD'S OWN OFFLINE MAP
            // ---------------------------------------------------------

            Intent intent =
                    new Intent(
                            SafeZonesActivity.this,
                            OfflineMapActivity.class
                    );

            // ---------------------------------------------------------
            // SEND SELECTED SAFE PLACE TO OFFLINE MAP
            // ---------------------------------------------------------

            intent.putExtra(
                    "safe_zone_latitude",
                    place.latitude
            );

            intent.putExtra(
                    "safe_zone_longitude",
                    place.longitude
            );

            intent.putExtra(
                    "safe_zone_name",
                    place.name == null
                            ? "Safe Place"
                            : place.name
            );

            intent.putExtra(
                    "safe_zone_address",
                    place.address == null
                            ? ""
                            : place.address
            );

            intent.putExtra(
                    "open_safe_zone",
                    true
            );

            // ---------------------------------------------------------
            // START YOUR LOCAL OFFLINE MAP
            // ---------------------------------------------------------

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    SafeZonesActivity.this,
                    "Unable to open offline map",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ============================================================
    // SHOW COORDINATES
    // ============================================================

    private void showCoordinates(
            SafePlace place) {

        Toast.makeText(

                this,

                place.name
                        + "\n"
                        + String.format(
                        Locale.getDefault(),
                        "%.6f, %.6f",
                        place.latitude,
                        place.longitude
                ),

                Toast.LENGTH_LONG

        ).show();
    }

    // ============================================================
    // CALL
    // ============================================================

    private void callPlace(
            SafePlace place) {

        if (
                place == null ||
                        place.phoneNumber == null ||
                        place.phoneNumber.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Phone number unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            Intent intent =
                    new Intent(

                            Intent.ACTION_DIAL,

                            Uri.parse(
                                    "tel:"
                                            + place.phoneNumber
                            )
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open dialer",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ============================================================
    // LOCATION ERROR
    // ============================================================

    private void showLocationError() {

        stopLocationUpdates();

        handler.removeCallbacks(
                locationTimeoutRunnable
        );

        hideLoading();

        if (tvCurrentLocation != null) {

            tvCurrentLocation.setText(
                    "Location unavailable"
            );
        }

        if (tvResultCount != null) {

            tvResultCount.setText(
                    "Enable Location and tap Refresh"
            );
        }

        Toast.makeText(
                this,
                "Unable to get location. Please enable Location.",
                Toast.LENGTH_LONG
        ).show();
    }

    // ============================================================
    // LOADING
    // ============================================================

    private void showLoading() {

        if (progressBar != null) {

            progressBar.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void hideLoading() {

        if (progressBar != null) {

            progressBar.setVisibility(
                    View.GONE
            );
        }
    }

    // ============================================================
    // STOP LOCATION
    // ============================================================

    private void stopLocationUpdates() {

        try {

            if (locationManager != null) {

                boolean fineGranted =
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        )
                                == PackageManager.PERMISSION_GRANTED;

                boolean coarseGranted =
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                                == PackageManager.PERMISSION_GRANTED;

                if (fineGranted || coarseGranted) {

                    locationManager.removeUpdates(
                            locationListener
                    );
                }
            }

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // PERMISSION RESULT
    // ============================================================

    @Override
    public void onRequestPermissionsResult(

            int requestCode,

            @NonNull String[] permissions,

            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(

                requestCode,

                permissions,

                grantResults
        );

        if (
                requestCode
                        ==
                        LOCATION_PERMISSION_REQUEST_CODE
        ) {

            boolean granted = false;

            for (int result : grantResults) {

                if (
                        result
                                ==
                                PackageManager.PERMISSION_GRANTED
                ) {

                    granted = true;

                    break;
                }
            }

            if (granted) {

                getCurrentLocation();

            } else {

                hideLoading();

                if (tvCurrentLocation != null) {

                    tvCurrentLocation.setText(
                            "Location permission denied"
                    );
                }

                if (tvResultCount != null) {

                    tvResultCount.setText(
                            "Allow location permission to continue"
                    );
                }
            }
        }
    }

    // ============================================================
    // RESUME
    // ============================================================

    @Override
    protected void onResume() {

        super.onResume();

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED;

        if (currentLocation == null &&
                (fineGranted || coarseGranted)) {

            getCurrentLocation();
        }
    }

    // ============================================================
    // DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );

        stopLocationUpdates();

        super.onDestroy();
    }

    // ============================================================
    // SAFE PLACE MODEL
    // ============================================================

    public static class SafePlace {

        public String id;

        public String name;

        public String category;

        public String address;

        public String phoneNumber;

        public double latitude;

        public double longitude;

        public double distanceKm;

        public SafePlace(

                String id,

                String name,

                String category,

                String address,

                String phoneNumber,

                double latitude,

                double longitude) {

            this.id = id;

            this.name = name;

            this.category = category;

            this.address = address;

            this.phoneNumber = phoneNumber;

            this.latitude = latitude;

            this.longitude = longitude;

            this.distanceKm = 0.0;
        }
    }
}