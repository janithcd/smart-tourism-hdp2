package lk.janith.smart_tourism.fragment;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.adapter.AttractionAdapter;
import lk.janith.smart_tourism.adapter.StopChipAdapter;
import lk.janith.smart_tourism.model.AttractionPlace;
import lk.janith.smart_tourism.model.RouteStop;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_PACKAGE_NAME = "package_name";
    private static final String ROUTES_ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final String PLACES_NEARBY_ENDPOINT = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String DEFAULT_PACKAGE = "5 Days";
    private static final double ATTRACTION_RADIUS_METERS = 25000.0;

    private GoogleMap googleMap;
    private TextView txtPackageTitle;
    private TextView txtPackageInfo;
    private MaterialButtonToggleGroup tourToggleGroup;
    private RecyclerView recyclerStops;
    private RecyclerView recyclerAttractions;

    private StopChipAdapter stopChipAdapter;
    private AttractionAdapter attractionAdapter;

    private final List<RouteStop> currentStops = new ArrayList<>();
    private final List<AttractionPlace> currentAttractions = new ArrayList<>();
    private final List<Circle> stopCircles = new ArrayList<>();
    private final HashMap<String, Marker> attractionMarkers = new HashMap<>();
    private final HashMap<String, List<AttractionPlace>> attractionCache = new HashMap<>();

    private String selectedPackageName = DEFAULT_PACKAGE;
    private String currentRouteSummary = "";

    public MapFragment() {
        super(R.layout.fragment_map);
    }

    public static MapFragment newInstance(String packageName) {
        MapFragment fragment = new MapFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_PACKAGE_NAME, packageName);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtPackageTitle = view.findViewById(R.id.txtPackageTitle);
        txtPackageInfo = view.findViewById(R.id.txtPackageInfo);
        tourToggleGroup = view.findViewById(R.id.tourToggleGroup);
        recyclerStops = view.findViewById(R.id.recyclerStops);
        recyclerAttractions = view.findViewById(R.id.recyclerAttractions);

        recyclerStops.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerAttractions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        stopChipAdapter = new StopChipAdapter(currentStops, this::selectStop);
        attractionAdapter = new AttractionAdapter(currentAttractions, this::focusAttraction);

        recyclerStops.setAdapter(stopChipAdapter);
        recyclerAttractions.setAdapter(attractionAdapter);

        if (getArguments() != null) {
            String packageName = getArguments().getString(ARG_PACKAGE_NAME);
            if (!TextUtils.isEmpty(packageName)) {
                selectedPackageName = packageName;
            }
        }

        setSelectedTourButton(selectedPackageName);

        tourToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btnTour5) {
                loadPackageRoute("5 Days");
            } else if (checkedId == R.id.btnTour7) {
                loadPackageRoute("7 Days");
            } else if (checkedId == R.id.btnTour10) {
                loadPackageRoute("10 Days");
            } else if (checkedId == R.id.btnTour14) {
                loadPackageRoute("14 Days");
            }
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapContainer);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();

            if (tag instanceof Integer) {
                selectStop((Integer) tag);
                return true;
            }

            if (tag instanceof String) {
                AttractionPlace match = findAttractionById((String) tag);
                if (match != null) {
                    marker.showInfoWindow();
                    focusAttraction(match);
                }
            }

            return false;
        });

        loadPackageRoute(selectedPackageName);
    }

    private void setSelectedTourButton(String packageName) {
        if (tourToggleGroup == null) return;

        if ("5 Days".equalsIgnoreCase(packageName)) {
            tourToggleGroup.check(R.id.btnTour5);
        } else if ("7 Days".equalsIgnoreCase(packageName)) {
            tourToggleGroup.check(R.id.btnTour7);
        } else if ("10 Days".equalsIgnoreCase(packageName)) {
            tourToggleGroup.check(R.id.btnTour10);
        } else if ("14 Days".equalsIgnoreCase(packageName)) {
            tourToggleGroup.check(R.id.btnTour14);
        }
    }

    public void loadPackageRoute(String packageName) {
        selectedPackageName = packageName;
        attractionCache.clear();

        if (txtPackageTitle != null) {
            txtPackageTitle.setText(packageName + " Tour");
        }

        List<LatLng> routePoints = getRoutePoints(packageName);
        List<String> placeNames = getPlaceNames(packageName);

        currentStops.clear();
        for (int i = 0; i < routePoints.size(); i++) {
            currentStops.add(new RouteStop(placeNames.get(i), routePoints.get(i), i == 0));
        }
        stopChipAdapter.notifyDataSetChanged();

        if (googleMap == null) return;

        if (routePoints.isEmpty()) {
            txtPackageInfo.setText("No route data available");
            return;
        }

        txtPackageInfo.setText(getString(R.string.loading_route));
        requestRoutesApi(routePoints, placeNames);
    }

    private void requestRoutesApi(List<LatLng> routePoints, List<String> placeNames) {
        new Thread(() -> {
            try {
                String apiKey = getMapsApiKey();
                if (TextUtils.isEmpty(apiKey)) {
                    requireActivity().runOnUiThread(() -> {
                        txtPackageInfo.setText("Maps API key missing");
                        Toast.makeText(requireContext(), "Maps API key missing", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONObject requestBody = buildRoutesRequestBody(routePoints);

                URL url = new URL(ROUTES_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-Goog-Api-Key", apiKey);
                connection.setRequestProperty("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline");
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(requestBody.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();

                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                connection.disconnect();

                String responseText = responseBuilder.toString();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject responseJson = new JSONObject(responseText);
                    JSONArray routes = responseJson.optJSONArray("routes");

                    if (routes != null && routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        int distanceMeters = route.optInt("distanceMeters", 0);
                        String duration = route.optString("duration", "0s");

                        JSONObject polylineObject = route.optJSONObject("polyline");
                        String encodedPolyline = polylineObject != null
                                ? polylineObject.optString("encodedPolyline", "")
                                : "";

                        requireActivity().runOnUiThread(() -> {
                            drawRoutesApiResult(routePoints, placeNames, encodedPolyline, distanceMeters, duration);
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> fallbackDraw(routePoints, placeNames));
                    }
                } else {
                    requireActivity().runOnUiThread(() -> fallbackDraw(routePoints, placeNames));
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> fallbackDraw(routePoints, placeNames));
            }
        }).start();
    }

    private void drawRoutesApiResult(List<LatLng> routePoints, List<String> placeNames,
                                     String encodedPolyline, int distanceMeters, String durationRaw) {

        googleMap.clear();
        stopCircles.clear();
        clearAttractionMarkers();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            boundsBuilder.include(point);

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(point)
                            .title(placeNames.get(i))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            );

            if (marker != null) {
                marker.setTag(i);
            }

            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(point)
                    .radius(ATTRACTION_RADIUS_METERS)
                    .strokeWidth(2f)
                    .strokeColor(Color.argb(120, 25, 118, 210))
                    .fillColor(Color.argb(35, 25, 118, 210)));
            stopCircles.add(circle);
        }

        List<LatLng> decodedPoints;
        if (!TextUtils.isEmpty(encodedPolyline)) {
            decodedPoints = PolyUtil.decode(encodedPolyline);
        } else {
            decodedPoints = routePoints;
        }

        googleMap.addPolyline(
                new PolylineOptions()
                        .addAll(decodedPoints)
                        .width(9f)
                        .color(requireContext().getColor(R.color.md_theme_primary))
                        .jointType(JointType.ROUND)
        );

        double km = distanceMeters / 1000.0;
        currentRouteSummary = "Route: " + String.format(Locale.getDefault(), "%.1f km", km)
                + " • Duration: " + formatDuration(durationRaw);

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
        selectStop(0);
    }

    private void fallbackDraw(List<LatLng> routePoints, List<String> placeNames) {
        googleMap.clear();
        stopCircles.clear();
        clearAttractionMarkers();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            boundsBuilder.include(point);

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(point)
                            .title(placeNames.get(i))
            );

            if (marker != null) {
                marker.setTag(i);
            }

            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(point)
                    .radius(ATTRACTION_RADIUS_METERS)
                    .strokeWidth(2f)
                    .strokeColor(Color.argb(120, 25, 118, 210))
                    .fillColor(Color.argb(35, 25, 118, 210)));
            stopCircles.add(circle);
        }

        googleMap.addPolyline(
                new PolylineOptions()
                        .addAll(routePoints)
                        .width(8f)
                        .color(requireContext().getColor(R.color.md_theme_primary))
                        .geodesic(true)
                        .jointType(JointType.ROUND)
        );

        double km = calculateStraightDistanceKm(routePoints);
        currentRouteSummary = "Estimated Route: " + String.format(Locale.getDefault(), "%.1f km", km);

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
        selectStop(0);
    }

    private void selectStop(int position) {
        if (position < 0 || position >= currentStops.size()) return;

        for (int i = 0; i < currentStops.size(); i++) {
            currentStops.get(i).setSelected(i == position);
        }
        stopChipAdapter.notifyDataSetChanged();
        updateCircleStyles(position);

        RouteStop stop = currentStops.get(position);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(stop.getLatLng(), 11f));

        List<AttractionPlace> cached = attractionCache.get(stop.getName());
        if (cached != null) {
            currentAttractions.clear();
            currentAttractions.addAll(cached);
            attractionAdapter.notifyDataSetChanged();
            renderAttractionMarkers();
            updateInfo(stop.getName(), currentAttractions.isEmpty()
                    ? getString(R.string.map_no_attractions)
                    : currentAttractions.size() + " nearby attractions within 25 km");
            return;
        }

        updateInfo(stop.getName(), getString(R.string.map_loading_attractions));
        fetchNearbyAttractions(stop);
    }

    private void updateCircleStyles(int selectedIndex) {
        for (int i = 0; i < stopCircles.size(); i++) {
            Circle circle = stopCircles.get(i);
            if (i == selectedIndex) {
                circle.setStrokeColor(Color.argb(210, 25, 118, 210));
                circle.setFillColor(Color.argb(55, 25, 118, 210));
            } else {
                circle.setStrokeColor(Color.argb(120, 25, 118, 210));
                circle.setFillColor(Color.argb(25, 25, 118, 210));
            }
        }
    }

    private void fetchNearbyAttractions(RouteStop stop) {
        new Thread(() -> {
            try {
                String apiKey = getMapsApiKey();
                if (TextUtils.isEmpty(apiKey)) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Maps API key missing", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                JSONObject body = new JSONObject();
                JSONArray includedTypes = new JSONArray();
                includedTypes.put("tourist_attraction");
                includedTypes.put("museum");
                includedTypes.put("art_gallery");
                includedTypes.put("church");
                includedTypes.put("mosque");
                includedTypes.put("hindu_temple");
                includedTypes.put("buddhist_temple");

                body.put("includedTypes", includedTypes);
                body.put("maxResultCount", 8);

                JSONObject center = new JSONObject();
                center.put("latitude", stop.getLatLng().latitude);
                center.put("longitude", stop.getLatLng().longitude);

                JSONObject circle = new JSONObject();
                circle.put("center", center);
                circle.put("radius", ATTRACTION_RADIUS_METERS);

                JSONObject locationRestriction = new JSONObject();
                locationRestriction.put("circle", circle);

                body.put("locationRestriction", locationRestriction);

                URL url = new URL(PLACES_NEARBY_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-Goog-Api-Key", apiKey);
                connection.setRequestProperty(
                        "X-Goog-FieldMask",
                        "places.id,places.displayName,places.formattedAddress,places.location," +
                                "places.rating,places.userRatingCount,places.primaryTypeDisplayName"
                );
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                connection.disconnect();

                String responseText = responseBuilder.toString();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(responseText);
                    JSONArray places = json.optJSONArray("places");
                    List<AttractionPlace> fresh = new ArrayList<>();

                    if (places != null) {
                        for (int i = 0; i < places.length(); i++) {
                            JSONObject obj = places.getJSONObject(i);

                            String id = obj.optString("id", "");
                            String name = obj.optJSONObject("displayName") != null
                                    ? obj.optJSONObject("displayName").optString("text", "")
                                    : "";
                            String address = obj.optString("formattedAddress", "");
                            String subtitle = obj.optJSONObject("primaryTypeDisplayName") != null
                                    ? obj.optJSONObject("primaryTypeDisplayName").optString("text", "Attraction")
                                    : "Attraction";
                            double rating = obj.optDouble("rating", 0.0);
                            int userRatingCount = obj.optInt("userRatingCount", 0);

                            JSONObject location = obj.optJSONObject("location");
                            if (location == null) continue;

                            double lat = location.optDouble("latitude", 0);
                            double lng = location.optDouble("longitude", 0);

                            fresh.add(new AttractionPlace(
                                    id,
                                    name,
                                    subtitle,
                                    address,
                                    rating,
                                    userRatingCount,
                                    new LatLng(lat, lng)
                            ));
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        attractionCache.put(stop.getName(), fresh);
                        currentAttractions.clear();
                        currentAttractions.addAll(fresh);
                        attractionAdapter.notifyDataSetChanged();
                        renderAttractionMarkers();

                        updateInfo(stop.getName(),
                                fresh.isEmpty()
                                        ? getString(R.string.map_no_attractions)
                                        : fresh.size() + " nearby attractions within 25 km");
                    });
                } else {
                    requireActivity().runOnUiThread(() ->
                            updateInfo(stop.getName(), getString(R.string.map_no_attractions))
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Attraction error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void renderAttractionMarkers() {
        clearAttractionMarkers();

        for (AttractionPlace place : currentAttractions) {
            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName())
                            .snippet(place.getSubtitle())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            );

            if (marker != null) {
                marker.setTag(place.getId());
                attractionMarkers.put(place.getId(), marker);
            }
        }
    }

    private void clearAttractionMarkers() {
        for (Marker marker : attractionMarkers.values()) {
            marker.remove();
        }
        attractionMarkers.clear();
    }

    private AttractionPlace findAttractionById(String id) {
        for (AttractionPlace place : currentAttractions) {
            if (place.getId().equals(id)) {
                return place;
            }
        }
        return null;
    }

    private void focusAttraction(AttractionPlace place) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 13.5f));
        Marker marker = attractionMarkers.get(place.getId());
        if (marker != null) {
            marker.showInfoWindow();
        }
    }

    private void updateInfo(String stopName, String extra) {
        txtPackageInfo.setText(currentRouteSummary + "\n" + stopName + " • " + extra);
    }

    // KEEP your existing methods below unchanged:
    // buildRoutesRequestBody(...)
    // createWaypoint(...)
    // formatDuration(...)
    // getMapsApiKey(...)
    // getRoutePoints(...)
    // getPlaceNames(...)
    // calculateStraightDistanceKm(...)

    private JSONObject buildRoutesRequestBody(List<LatLng> points) throws Exception {
        JSONObject request = new JSONObject();

        request.put("origin", createWaypoint(points.get(0)));
        request.put("destination", createWaypoint(points.get(points.size() - 1)));
        request.put("travelMode", "DRIVE");
        request.put("routingPreference", "TRAFFIC_AWARE");
        request.put("polylineQuality", "HIGH_QUALITY");
        request.put("languageCode", "en-US");
        request.put("units", "METRIC");

        JSONArray intermediates = new JSONArray();
        for (int i = 1; i < points.size() - 1; i++) {
            intermediates.put(createWaypoint(points.get(i)));
        }
        request.put("intermediates", intermediates);

        return request;
    }

    private JSONObject createWaypoint(LatLng latLng) throws Exception {
        JSONObject waypoint = new JSONObject();
        JSONObject location = new JSONObject();
        JSONObject latLngObject = new JSONObject();

        latLngObject.put("latitude", latLng.latitude);
        latLngObject.put("longitude", latLng.longitude);

        location.put("latLng", latLngObject);
        waypoint.put("location", location);

        return waypoint;
    }

    private String formatDuration(String durationRaw) {
        try {
            String secondsString = durationRaw.replace("s", "");
            long totalSeconds = Long.parseLong(secondsString);

            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;

            if (hours > 0) {
                return hours + "h " + minutes + "m";
            } else {
                return minutes + "m";
            }
        } catch (Exception e) {
            return durationRaw;
        }
    }

    private String getMapsApiKey() {
        try {
            ApplicationInfo appInfo = requireContext()
                    .getPackageManager()
                    .getApplicationInfo(requireContext().getPackageName(), PackageManager.GET_META_DATA);

            if (appInfo.metaData != null) {
                return appInfo.metaData.getString("com.google.android.geo.API_KEY", "");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private List<LatLng> getRoutePoints(String packageName) {
        List<LatLng> points = new ArrayList<>();

        if ("5 Days".equalsIgnoreCase(packageName)) {
            points.add(new LatLng(6.9271, 79.8612));   // Colombo
            points.add(new LatLng(7.9570, 80.7603));   // Sigiriya
            points.add(new LatLng(7.2906, 80.6337));   // Kandy
            points.add(new LatLng(6.8667, 81.0466));   // Ella
            points.add(new LatLng(6.0535, 80.2210));   // Galle

        } else if ("7 Days".equalsIgnoreCase(packageName)) {
            points.add(new LatLng(7.2083, 79.8358));   // Negombo
            points.add(new LatLng(7.9570, 80.7603));   // Sigiriya
            points.add(new LatLng(7.2906, 80.6337));   // Kandy
            points.add(new LatLng(6.9497, 80.7891));   // Nuwara Eliya
            points.add(new LatLng(6.8667, 81.0466));   // Ella
            points.add(new LatLng(6.3725, 81.5185));   // Yala
            points.add(new LatLng(6.0535, 80.2210));   // Galle

        } else if ("10 Days".equalsIgnoreCase(packageName)) {
            points.add(new LatLng(7.2083, 79.8358));   // Negombo
            points.add(new LatLng(8.3114, 80.4037));   // Anuradhapura
            points.add(new LatLng(7.9570, 80.7603));   // Sigiriya
            points.add(new LatLng(7.2906, 80.6337));   // Kandy
            points.add(new LatLng(6.9497, 80.7891));   // Nuwara Eliya
            points.add(new LatLng(6.8667, 81.0466));   // Ella
            points.add(new LatLng(6.3725, 81.5185));   // Yala
            points.add(new LatLng(5.9485, 80.5353));   // Mirissa
            points.add(new LatLng(6.0535, 80.2210));   // Galle
            points.add(new LatLng(6.9271, 79.8612));   // Colombo

        } else if ("14 Days".equalsIgnoreCase(packageName)) {
            points.add(new LatLng(7.2083, 79.8358));   // Negombo
            points.add(new LatLng(8.3114, 80.4037));   // Anuradhapura
            points.add(new LatLng(7.9570, 80.7603));   // Sigiriya
            points.add(new LatLng(7.9403, 81.0188));   // Polonnaruwa
            points.add(new LatLng(7.2906, 80.6337));   // Kandy
            points.add(new LatLng(6.9497, 80.7891));   // Nuwara Eliya
            points.add(new LatLng(6.8667, 81.0466));   // Ella
            points.add(new LatLng(6.8406, 81.8369));   // Arugam Bay
            points.add(new LatLng(6.3725, 81.5185));   // Yala
            points.add(new LatLng(5.9485, 80.5353));   // Mirissa
            points.add(new LatLng(6.0535, 80.2210));   // Galle
            points.add(new LatLng(6.4218, 79.9957));   // Bentota
            points.add(new LatLng(6.9271, 79.8612));   // Colombo
        }

        return points;
    }

    private List<String> getPlaceNames(String packageName) {
        List<String> names = new ArrayList<>();

        if ("5 Days".equalsIgnoreCase(packageName)) {
            names.add("Colombo");
            names.add("Sigiriya");
            names.add("Kandy");
            names.add("Ella");
            names.add("Galle");

        } else if ("7 Days".equalsIgnoreCase(packageName)) {
            names.add("Negombo");
            names.add("Sigiriya");
            names.add("Kandy");
            names.add("Nuwara Eliya");
            names.add("Ella");
            names.add("Yala");
            names.add("Galle");

        } else if ("10 Days".equalsIgnoreCase(packageName)) {
            names.add("Negombo");
            names.add("Anuradhapura");
            names.add("Sigiriya");
            names.add("Kandy");
            names.add("Nuwara Eliya");
            names.add("Ella");
            names.add("Yala");
            names.add("Mirissa");
            names.add("Galle");
            names.add("Colombo");

        } else if ("14 Days".equalsIgnoreCase(packageName)) {
            names.add("Negombo");
            names.add("Anuradhapura");
            names.add("Sigiriya");
            names.add("Polonnaruwa");
            names.add("Kandy");
            names.add("Nuwara Eliya");
            names.add("Ella");
            names.add("Arugam Bay");
            names.add("Yala");
            names.add("Mirissa");
            names.add("Galle");
            names.add("Bentota");
            names.add("Colombo");
        }

        return names;
    }

    private double calculateStraightDistanceKm(List<LatLng> points) {
        float[] results = new float[1];
        double totalMeters = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            LatLng start = points.get(i);
            LatLng end = points.get(i + 1);

            android.location.Location.distanceBetween(
                    start.latitude, start.longitude,
                    end.latitude, end.longitude,
                    results
            );
            totalMeters += results[0];
        }

        return totalMeters / 1000.0;
    }
}