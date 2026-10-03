package lk.janith.smart_tourism.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.adapter.NearbyPlaceAdapter;

/** Searches only after a user action; location and result details are kept in memory. */
public class NearbyPlacesFragment extends Fragment {

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final List<Place.Field> PLACE_FIELDS = Arrays.asList(
            Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION,
            Place.Field.FORMATTED_ADDRESS
    );

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                if (getView() == null) return;
                if (hasLocationPermission()) {
                    findCurrentLocation();
                } else {
                    setStatus(R.string.nearby_permission_denied);
                }
            });

    private final Map<String, Marker> placeMarkers = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable delayedSearch = this::searchNearby;
    private FusedLocationProviderClient locationClient;
    private PlacesClient placesClient;
    private CancellationTokenSource locationCancellation;
    private CancellationTokenSource searchCancellation;
    private GoogleMap map;
    private Location currentLocation;
    private NearbyPlaceAdapter adapter;
    private MaterialButton findButton;
    private TextView statusView;
    private TextView locationView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private ChipGroup categories;
    private int searchGeneration;
    private int locationGeneration;
    private boolean permissionRequested;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby_places, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findButton = view.findViewById(R.id.btnFindNearby);
        statusView = view.findViewById(R.id.nearbyStatus);
        locationView = view.findViewById(R.id.nearbyLocation);
        emptyView = view.findViewById(R.id.nearbyEmpty);
        progressBar = view.findViewById(R.id.nearbyProgress);
        categories = view.findViewById(R.id.nearbyCategories);

        RecyclerView results = view.findViewById(R.id.nearbyResults);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NearbyPlaceAdapter(this::focusPlace, this::openDirections);
        results.setAdapter(adapter);

        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        findButton.setOnClickListener(v -> startLocationFlow());
        categories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (currentLocation != null && !checkedIds.isEmpty()) {
                mainHandler.removeCallbacks(delayedSearch);
                cancelSearch();
                adapter.clear();
                renderMap(adapter.getPlaces());
                setStatus(R.string.nearby_searching);
                emptyView.setVisibility(View.GONE);
                // Avoid sending several billable queries during quick category changes.
                mainHandler.postDelayed(delayedSearch, 400);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.nearbyMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                if (getView() == null) return;
                map = googleMap;
                renderMap(adapter.getPlaces());
            });
        }

        String key = getMapsApiKey();
        if (key.isEmpty()) {
            findButton.setEnabled(false);
            setStatus(R.string.nearby_key_missing);
            return;
        }
        try {
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(requireContext().getApplicationContext(), key);
            }
            placesClient = Places.createClient(requireContext());
        } catch (RuntimeException error) {
            findButton.setEnabled(false);
            setStatus(R.string.nearby_setup_failed);
        }
    }

    private String getMapsApiKey() {
        try {
            Bundle metadata = requireContext().getPackageManager().getApplicationInfo(
                    requireContext().getPackageName(), PackageManager.GET_META_DATA).metaData;
            String key = metadata == null ? null : metadata.getString("com.google.android.geo.API_KEY");
            return key == null ? "" : key.trim();
        } catch (PackageManager.NameNotFoundException error) {
            return "";
        }
    }

    private boolean hasLocationPermission() {
        Context context = requireContext();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationFlow() {
        if (!hasLocationPermission()) {
            if (permissionRequested
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(settings);
            } else {
                permissionRequested = true;
                permissionLauncher.launch(LOCATION_PERMISSIONS);
            }
            return;
        }
        findCurrentLocation();
    }

    @SuppressLint("MissingPermission") // Both foreground permissions are checked before this call.
    private void findCurrentLocation() {
        if (!hasLocationPermission() || getView() == null) return;
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (manager == null || (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            setStatus(R.string.nearby_location_off);
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.nearby_location_off)
                    .setPositiveButton(R.string.nearby_location_settings, (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        if (locationCancellation != null) locationCancellation.cancel();
        mainHandler.removeCallbacks(delayedSearch);
        cancelSearch();
        final int generation = ++locationGeneration;
        currentLocation = null;
        adapter.clear();
        renderMap(adapter.getPlaces());
        emptyView.setVisibility(View.GONE);
        locationView.setVisibility(View.GONE);
        locationCancellation = new CancellationTokenSource();
        setBusy(true);
        setStatus(R.string.nearby_locating);
        boolean precise = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(precise ? Priority.PRIORITY_HIGH_ACCURACY
                        : Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setMaxUpdateAgeMillis(30_000)
                .setDurationMillis(15_000)
                .build();
        locationClient.getCurrentLocation(request, locationCancellation.getToken())
                .addOnSuccessListener(location -> {
                    if (getView() == null || generation != locationGeneration) return;
                    locationCancellation = null;
                    if (location == null) {
                        setBusy(false);
                        setStatus(R.string.nearby_no_location);
                        return;
                    }
                    currentLocation = location;
                    int accuracy = location.hasAccuracy() ? Math.round(location.getAccuracy()) : 0;
                    locationView.setText(getString(precise ? R.string.nearby_coordinates
                                    : R.string.nearby_coordinates_approximate,
                            location.getLatitude(), location.getLongitude(), accuracy));
                    locationView.setVisibility(View.VISIBLE);
                    renderMap(adapter.getPlaces());
                    searchNearby();
                })
                .addOnFailureListener(error -> {
                    if (getView() == null || generation != locationGeneration) return;
                    locationCancellation = null;
                    setBusy(false);
                    setStatus(R.string.nearby_location_failed);
                });
    }

    private void searchNearby() {
        if (placesClient == null || currentLocation == null || getView() == null) return;
        cancelSearch();
        final int generation = searchGeneration;
        final Location searchLocation = currentLocation;
        List<String> types;
        int category = categories.getCheckedChipId();
        if (category == R.id.nearbyFood) {
            types = Arrays.asList("restaurant", "cafe");
        } else if (category == R.id.nearbyStays) {
            types = Arrays.asList("lodging", "hotel", "hostel");
        } else if (category == R.id.nearbyHelp) {
            types = Arrays.asList("hospital", "pharmacy", "police");
        } else {
            types = Arrays.asList("tourist_attraction", "museum", "park");
        }

        // Approximate locations need a larger area to include places near the real device.
        double accuracy = searchLocation.hasAccuracy() ? searchLocation.getAccuracy() : 0;
        double radius = Math.min(20_000, Math.max(5_000, accuracy * 2));
        LatLng center = new LatLng(searchLocation.getLatitude(), searchLocation.getLongitude());
        searchCancellation = new CancellationTokenSource();
        SearchNearbyRequest request = SearchNearbyRequest.builder(
                        CircularBounds.newInstance(center, radius), PLACE_FIELDS)
                .setIncludedTypes(types)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                .setMaxResultCount(12)
                .setCancellationToken(searchCancellation.getToken())
                .build();
        setBusy(true);
        setStatus(R.string.nearby_searching);
        emptyView.setVisibility(View.GONE);

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    if (getView() == null || generation != searchGeneration) return;
                    searchCancellation = null;
                    setBusy(false);
                    List<Place> places = response.getPlaces();
                    adapter.submit(places, searchLocation);
                    renderMap(places);
                    setStatus(places.isEmpty() ? R.string.nearby_no_results : R.string.nearby_results_ready);
                    emptyView.setVisibility(places.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(error -> {
                    if (getView() == null || generation != searchGeneration) return;
                    searchCancellation = null;
                    setBusy(false);
                    adapter.clear();
                    renderMap(adapter.getPlaces());
                    setStatus(R.string.nearby_search_failed);
                    emptyView.setVisibility(View.GONE);
                });
    }

    private void cancelSearch() {
        searchGeneration++;
        if (searchCancellation != null) {
            searchCancellation.cancel();
            searchCancellation = null;
        }
    }

    private void renderMap(List<Place> places) {
        if (map == null) return;
        map.clear();
        placeMarkers.clear();
        if (currentLocation == null) return;
        LatLng center = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        map.addMarker(new MarkerOptions().position(center)
                .title(getString(R.string.nearby_you))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center,
                currentLocation.hasAccuracy() && currentLocation.getAccuracy() > 5_000 ? 10f : 12f));
        for (Place place : places) {
            if (place.getLocation() == null) continue;
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(place.getLocation()).title(place.getDisplayName()));
            if (marker != null && place.getId() != null) placeMarkers.put(place.getId(), marker);
        }
    }

    private void focusPlace(Place place) {
        if (map == null || place.getLocation() == null) return;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLocation(), 15f));
        Marker marker = placeMarkers.get(place.getId());
        if (marker != null) marker.showInfoWindow();
    }

    private void openDirections(Place place) {
        if (place.getLocation() == null) return;
        String destination = place.getLocation().latitude + "," + place.getLocation().longitude;
        Uri url = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", destination)
                .build();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, url));
        } catch (ActivityNotFoundException error) {
            setStatus(R.string.nearby_no_maps_app);
        }
    }

    private void setStatus(int message) {
        statusView.setText(message);
    }

    private void setBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        findButton.setEnabled(!busy);
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(delayedSearch);
        locationGeneration++;
        if (locationCancellation != null) {
            locationCancellation.cancel();
            locationCancellation = null;
        }
        cancelSearch();
        map = null;
        currentLocation = null;
        placeMarkers.clear();
        placesClient = null;
        adapter = null;
        super.onDestroyView();
    }
}
